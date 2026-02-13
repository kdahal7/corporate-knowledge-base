package com.infosys.ai.kb.service;

import com.infosys.ai.kb.entity.Document;
import com.infosys.ai.kb.entity.DocumentChunk;
import com.infosys.ai.kb.repository.DocumentChunkRepository;
import com.infosys.ai.kb.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final GeminiService geminiService;

    @Value("${vector.chunk.size}")
    private int chunkSize;

    @Value("${vector.chunk.overlap}")
    private int chunkOverlap;

    /**
     * Upload and process a PDF document
     */
    @Transactional(noRollbackFor = Exception.class)
    public Document uploadDocument(MultipartFile file) throws IOException {
        log.info("Processing document: {}", file.getOriginalFilename());

        // Check if document already exists
        if (documentRepository.existsByFileName(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Document with this name already exists");
        }

        // Extract text from PDF
        String content = extractTextFromPdf(file);
        
        // Save document entity
        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .content(content)
                .totalChunks(0)
                .build();
        
        document = documentRepository.save(document);
        log.info("Document saved with ID: {}", document.getId());

        // Create chunks and generate embeddings
        List<String> chunks = createChunks(content);
        int chunkCount = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            
            try {
                // Generate embedding using Gemini
                float[] embedding = geminiService.generateEmbedding(chunkText);
                String embeddingStr = geminiService.floatArrayToVectorString(embedding);

                // Save chunk with embedding
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunkText)
                        .embedding(embeddingStr)
                        .build();
                
                chunkRepository.save(chunk);
                chunkRepository.flush(); // Force immediate save
                chunkCount++;
                
                log.info("Successfully saved chunk {}/{}", i + 1, chunks.size());
                
                // Rate limiting: sleep briefly between API calls
                Thread.sleep(100);
                
            } catch (Exception e) {
                log.error("Error processing chunk {}: {}", i, e.getMessage(), e);
                // Don't throw, continue with other chunks
            }
        }

        // Update document with total chunks
        document.setTotalChunks(chunkCount);
        documentRepository.save(document);

        log.info("Document processing complete. Created {} chunks", chunkCount);
        return document;
    }

    /**
     * Extract text content from PDF file
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        byte[] pdfBytes = file.getBytes();
        try (PDDocument pdfDocument = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);
            
            // Clean up text
            text = text.replaceAll("\\s+", " ").trim();
            
            log.info("Extracted {} characters from PDF", text.length());
            return text;
        }
    }

    /**
     * Split text into overlapping chunks
     */
    private List<String> createChunks(String text) {
        List<String> chunks = new ArrayList<>();
        
        // Split by sentences first
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (String sentence : sentences) {
            int sentenceLength = sentence.length();
            
            if (currentLength + sentenceLength > chunkSize && currentLength > 0) {
                // Save current chunk
                chunks.add(currentChunk.toString().trim());
                
                // Start new chunk with overlap
                String overlapText = getLastNCharacters(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
                currentLength = overlapText.length();
            }
            
            currentChunk.append(sentence).append(" ");
            currentLength += sentenceLength + 1;
        }

        // Add the last chunk
        if (currentLength > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Created {} chunks from document", chunks.size());
        return chunks;
    }

    /**
     * Get last N characters from text
     */
    private String getLastNCharacters(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        return text.substring(text.length() - n);
    }

    /**
     * Get all documents
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * Get document by ID
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    /**
     * Delete document
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document document = getDocumentById(id);
        documentRepository.delete(document);
        log.info("Deleted document: {}", document.getFileName());
    }
}
