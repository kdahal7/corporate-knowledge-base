# Database Setup Guide

## Option 1: Neon.tech (Recommended - Free & Managed)

### Why Neon?
- **Free Tier**: 3GB storage, perfect for demo
- **pgvector Pre-installed**: No manual setup needed
- **Global CDN**: Fast from anywhere
- **Auto-backup**: Point-in-time recovery

### Setup Steps:

1. **Sign Up**
   - Go to https://neon.tech/
   - Sign up with GitHub/Google
   - No credit card required

2. **Create Project**
   ```
   Project Name: corporate-kb
   Region: Choose closest to you
   PostgreSQL Version: 15
   ```

3. **Enable pgvector** (Auto-enabled)
   - Neon enables pgvector by default
   - Verify in SQL Editor:
     ```sql
     SELECT * FROM pg_extension WHERE extname = 'vector';
     ```

4. **Get Connection String**
   - Click "Connection Details"
   - Copy the JDBC URL:
     ```
     postgresql://[user]:[password]@[host]/[database]?sslmode=require
     ```

5. **Update application.properties**
   ```properties
   spring.datasource.url=jdbc:postgresql://YOUR-PROJECT.neon.tech:5432/YOUR-DB?sslmode=require
   spring.datasource.username=YOUR-USERNAME
   spring.datasource.password=YOUR-PASSWORD
   ```

## Option 2: Local PostgreSQL

### Prerequisites
- PostgreSQL 14+ installed
- Admin access to PostgreSQL

### Installation Steps

#### On Windows:

```powershell
# 1. Install PostgreSQL (if not installed)
# Download from: https://www.postgresql.org/download/windows/

# 2. Install pgvector
# Download from: https://github.com/pgvector/pgvector/releases
# Extract and run:
cd pgvector
make
make install

# 3. Connect to PostgreSQL
psql -U postgres

# 4. Create database
CREATE DATABASE corporate_kb;

# 5. Connect to database
\c corporate_kb

# 6. Enable pgvector
CREATE EXTENSION vector;

# 7. Verify
SELECT * FROM pg_extension WHERE extname = 'vector';
```

#### On macOS:

```bash
# 1. Install PostgreSQL via Homebrew
brew install postgresql@15

# 2. Install pgvector
brew install pgvector

# 3. Start PostgreSQL
brew services start postgresql@15

# 4. Create database
createdb corporate_kb

# 5. Connect and enable extension
psql corporate_kb
CREATE EXTENSION vector;
```

#### On Linux (Ubuntu/Debian):

```bash
# 1. Install PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# 2. Install build tools
sudo apt install build-essential postgresql-server-dev-15

# 3. Install pgvector
cd /tmp
git clone https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install

# 4. Create database
sudo -u postgres psql
CREATE DATABASE corporate_kb;
\c corporate_kb
CREATE EXTENSION vector;
```

### Local Connection String

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/corporate_kb
spring.datasource.username=postgres
spring.datasource.password=your_password
```

## Database Schema

The application will auto-create these tables:

### documents table
```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    content TEXT,
    uploaded_at TIMESTAMP NOT NULL,
    total_chunks INTEGER NOT NULL
);
```

### document_chunks table
```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunk_index ON document_chunks(chunk_index);
```

## Performance Optimization

### Add Vector Index (After inserting ~1000+ chunks)

```sql
-- IVFFlat index for faster similarity search
CREATE INDEX ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);
```

**When to use:**
- **< 1000 chunks**: No index needed (sequential scan is faster)
- **> 1000 chunks**: Add IVFFlat index
- **> 10000 chunks**: Consider HNSW index (PostgreSQL 15+)

## Monitoring Queries

### Check database size
```sql
SELECT pg_size_pretty(pg_database_size('corporate_kb'));
```

### Count documents and chunks
```sql
SELECT 
    (SELECT COUNT(*) FROM documents) as total_documents,
    (SELECT COUNT(*) FROM document_chunks) as total_chunks;
```

### View recent uploads
```sql
SELECT file_name, total_chunks, uploaded_at 
FROM documents 
ORDER BY uploaded_at DESC 
LIMIT 5;
```

### Test vector search
```sql
-- Find chunks similar to a query embedding
SELECT 
    dc.content,
    dc.embedding <=> '[0.1, 0.2, ...]'::vector as distance
FROM document_chunks dc
WHERE dc.embedding IS NOT NULL
ORDER BY distance ASC
LIMIT 3;
```

## Troubleshooting

### Error: "extension vector does not exist"

**Solution:**
```sql
-- Check if pgvector is installed
SELECT * FROM pg_available_extensions WHERE name = 'vector';

-- If not listed, reinstall pgvector
-- Follow installation steps above
```

### Error: "connection refused"

**Solution:**
1. Check PostgreSQL is running:
   ```bash
   # Linux/Mac
   pg_isready
   
   # Windows
   pg_ctl status
   ```

2. Check firewall settings
3. Verify connection string

### Slow queries

**Solution:**
```sql
-- Add indexes
CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops);

-- Analyze query performance
EXPLAIN ANALYZE
SELECT * FROM document_chunks
ORDER BY embedding <=> '[...]'::vector
LIMIT 3;
```

## Backup & Restore

### Backup
```bash
pg_dump -U postgres corporate_kb > backup.sql
```

### Restore
```bash
psql -U postgres corporate_kb < backup.sql
```

## Security Best Practices

1. **Never commit credentials** to version control
2. **Use environment variables** for production:
   ```bash
   export DB_URL=your_url
   export DB_USER=your_user
   export DB_PASSWORD=your_password
   ```

3. **Restrict PostgreSQL access**:
   ```sql
   -- Create read-only user for analytics
   CREATE USER analytics WITH PASSWORD 'secure_password';
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics;
   ```

4. **Enable SSL** for production (Neon does this by default)

## Next Steps

After database setup:
1. Update `application.properties` with your connection details
2. Run the Spring Boot application
3. Check logs for successful schema creation
4. Upload your first PDF document!
