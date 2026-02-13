package com.infosys.ai.kb.service;

import com.infosys.ai.kb.dto.QueryRequest;
import com.infosys.ai.kb.dto.QueryResponse;
import com.infosys.ai.kb.dto.SearchResult;
import com.infosys.ai.kb.entity.DocumentChunk;
import com.infosys.ai.kb.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private final GeminiService geminiService;
    private final DocumentChunkRepository chunkRepository;

    /**
     * Process a user query using RAG (Retrieval Augmented Generation)
     */
    public QueryResponse processQuery(QueryRequest request) throws IOException {
        log.info("Processing query: {}", request.getQuestion());

        // Step 1: Generate embedding for the query
        float[] queryEmbedding = geminiService.generateEmbedding(request.getQuestion());
        String queryEmbeddingStr = geminiService.floatArrayToVectorString(queryEmbedding);

        // Step 2: Find similar chunks using vector similarity search
        // Use fewer chunks for simple factual questions
        int topK = request.getTopK() != null ? request.getTopK() : 2;
        List<DocumentChunk> similarChunks = chunkRepository.findSimilarChunks(
            queryEmbeddingStr, 
            topK
        );

        log.info("Found {} similar chunks", similarChunks.size());

        // Step 3: Build context from retrieved chunks
        String context = similarChunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));

        // Step 4: Generate response using Gemini with context
        String answer;
        if (context.isEmpty()) {
            answer = "I couldn't find any relevant information in the knowledge base to answer your question.";
        } else {
            answer = geminiService.generateResponse(context, request.getQuestion());
        }

        // Step 5: Create search results
        List<SearchResult> searchResults = new ArrayList<>();
        for (DocumentChunk chunk : similarChunks) {
            SearchResult result = SearchResult.builder()
                    .documentId(chunk.getDocument().getId())
                    .documentName(chunk.getDocument().getFileName())
                    .chunkIndex(chunk.getChunkIndex())
                    .content(chunk.getContent())
                    .relevanceScore(calculateRelevanceScore(chunk))
                    .build();
            searchResults.add(result);
        }

        return QueryResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .sources(searchResults)
                .totalSources(searchResults.size())
                .build();
    }

    /**
     * Perform semantic search without generating an answer
     */
    public List<SearchResult> semanticSearch(String query, Integer topK) throws IOException {
        log.info("Performing semantic search: {}", query);

        // Generate embedding for the query
        float[] queryEmbedding = geminiService.generateEmbedding(query);
        String queryEmbeddingStr = geminiService.floatArrayToVectorString(queryEmbedding);

        // Find similar chunks
        int limit = topK != null ? topK : 5;
        List<DocumentChunk> similarChunks = chunkRepository.findSimilarChunks(
            queryEmbeddingStr, 
            limit
        );

        // Convert to search results
        return similarChunks.stream()
                .map(chunk -> SearchResult.builder()
                        .documentId(chunk.getDocument().getId())
                        .documentName(chunk.getDocument().getFileName())
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .relevanceScore(calculateRelevanceScore(chunk))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate relevance score (placeholder - in production, use actual distance metric)
     */
    private Double calculateRelevanceScore(DocumentChunk chunk) {
        // In a real implementation, this would be calculated from the distance
        // For now, return a placeholder score
        return 0.85;
    }
}
