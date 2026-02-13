package com.infosys.ai.kb.controller;

import com.infosys.ai.kb.dto.ApiResponse;
import com.infosys.ai.kb.dto.QueryRequest;
import com.infosys.ai.kb.dto.QueryResponse;
import com.infosys.ai.kb.dto.SearchResult;
import com.infosys.ai.kb.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final RAGService ragService;

    /**
     * Process a query using RAG
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QueryResponse>> processQuery(
            @RequestBody QueryRequest request) {
        try {
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Question cannot be empty"));
            }

            QueryResponse response = ragService.processQuery(request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process query: " + e.getMessage()));
        }
    }

    /**
     * Perform semantic search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResult>>> semanticSearch(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") Integer topK) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Query cannot be empty"));
            }

            List<SearchResult> results = ragService.semanticSearch(query, topK);
            return ResponseEntity.ok(ApiResponse.success(results));

        } catch (Exception e) {
            log.error("Error performing semantic search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to perform search: " + e.getMessage()));
        }
    }
}
