-- Database Initialization Script for Corporate Knowledge Base
-- Run this script on your PostgreSQL database before starting the application

-- Step 1: Enable pgvector extension (REQUIRED)
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify extension is installed
SELECT * FROM pg_extension WHERE extname = 'vector';

-- Step 2: Create database if not exists (run as superuser)
-- CREATE DATABASE corporate_kb;

-- Step 3: The application will automatically create these tables via JPA
-- This script documents the schema for reference

-- Documents table
-- Stores metadata about uploaded PDF documents
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    content TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_file_name UNIQUE (file_name)
);

-- Document chunks table
-- Stores text chunks with their vector embeddings
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE
);

-- Step 4: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_index ON document_chunks(chunk_index);
CREATE INDEX IF NOT EXISTS idx_uploaded_at ON documents(uploaded_at DESC);

-- Step 5: Create vector index for similarity search
-- Note: Only create this after inserting 1000+ chunks for better performance
-- For smaller datasets, sequential scan is actually faster

-- IVFFlat index (good for 1K-1M vectors)
-- CREATE INDEX IF NOT EXISTS idx_embedding_ivfflat
-- ON document_chunks
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- HNSW index (better for 1M+ vectors, requires PostgreSQL 15+)
-- CREATE INDEX IF NOT EXISTS idx_embedding_hnsw
-- ON document_chunks
-- USING hnsw (embedding vector_cosine_ops)
-- WITH (m = 16, ef_construction = 64);

-- Step 6: Create helpful views

-- View: Document statistics
CREATE OR REPLACE VIEW document_stats AS
SELECT
    d.id,
    d.file_name,
    d.total_chunks,
    d.file_size,
    d.uploaded_at,
    COUNT(dc.id) as actual_chunks,
    COUNT(dc.embedding) as chunks_with_embeddings,
    ROUND(AVG(LENGTH(dc.content))) as avg_chunk_size
FROM documents d
LEFT JOIN document_chunks dc ON d.id = dc.document_id
GROUP BY d.id, d.file_name, d.total_chunks, d.file_size, d.uploaded_at;

-- View: Recent uploads
CREATE OR REPLACE VIEW recent_uploads AS
SELECT
    file_name,
    total_chunks,
    ROUND(file_size / 1024.0, 2) as size_kb,
    uploaded_at
FROM documents
ORDER BY uploaded_at DESC
LIMIT 10;

-- Step 7: Create utility functions

-- Function: Calculate vector similarity
CREATE OR REPLACE FUNCTION calculate_similarity(vec1 vector(768), vec2 vector(768))
RETURNS FLOAT AS $$
BEGIN
    RETURN 1 - (vec1 <=> vec2);
END;
$$ LANGUAGE plpgsql;

-- Function: Search similar chunks
CREATE OR REPLACE FUNCTION search_similar_chunks(
    query_embedding vector(768),
    limit_count INTEGER DEFAULT 5
)
RETURNS TABLE (
    chunk_id BIGINT,
    document_name VARCHAR,
    chunk_content TEXT,
    similarity_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        dc.id,
        d.file_name,
        dc.content,
        calculate_similarity(dc.embedding, query_embedding) as score
    FROM document_chunks dc
    JOIN documents d ON dc.document_id = d.id
    WHERE dc.embedding IS NOT NULL
    ORDER BY dc.embedding <=> query_embedding
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Step 8: Grant permissions (adjust user as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO your_app_user;

-- Step 9: Verify setup
DO $$
BEGIN
    -- Check if vector extension is installed
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'pgvector extension is not installed!';
    END IF;

    -- Check if tables exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'documents') THEN
        RAISE NOTICE 'documents table will be created by JPA on application startup';
    END IF;

    RAISE NOTICE 'Database setup verification complete!';
END $$;

-- Step 10: Sample queries for testing

-- Count documents and chunks
SELECT
    (SELECT COUNT(*) FROM documents) as total_documents,
    (SELECT COUNT(*) FROM document_chunks) as total_chunks,
    (SELECT COUNT(*) FROM document_chunks WHERE embedding IS NOT NULL) as chunks_with_embeddings;

-- View document statistics
SELECT * FROM document_stats;

-- View recent uploads
SELECT * FROM recent_uploads;

-- Check database size
SELECT pg_size_pretty(pg_database_size(current_database())) as database_size;

-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Sample vector search (replace [...] with actual 768-dim vector)
-- SELECT
--     dc.content,
--     dc.embedding <=> '[0.1, 0.2, ..., 0.768]'::vector as distance
-- FROM document_chunks dc
-- WHERE dc.embedding IS NOT NULL
-- ORDER BY distance ASC
-- LIMIT 3;

COMMIT;

-- Success message
SELECT 'Database initialization script completed successfully!' as status;
