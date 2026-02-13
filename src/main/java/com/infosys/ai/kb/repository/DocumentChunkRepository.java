package com.infosys.ai.kb.repository;

import com.infosys.ai.kb.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    List<DocumentChunk> findByDocumentId(Long documentId);
    
    @Query(value = "SELECT dc.*, " +

    "(CAST(dc.embedding AS vector) <=> CAST(:queryEmbedding AS vector)) as distance " +
           "FROM document_chunks dc " +
           "WHERE dc.embedding IS NOT NULL " +
           "ORDER BY distance ASC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
        @Param("queryEmbedding") String queryEmbedding, 
        @Param("limit") int limit
    );
    
    long countByDocumentId(Long documentId);
}
