package com.infosys.ai.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String content;
    private Double relevanceScore;
}
