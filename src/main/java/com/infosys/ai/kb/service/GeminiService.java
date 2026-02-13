package com.infosys.ai.kb.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
// import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeminiService {

    @Value("${llm.provider:ollama}")
    private String llmProvider;

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.model.name}")
    private String ollamaModelName;

    @Value("${huggingface.api.key}")
    private String huggingfaceApiKey;

    @Value("${huggingface.api.url}")
    private String huggingfaceApiUrl;

    @Value("${huggingface.model.name}")
    private String huggingfaceModelName;

    private final OkHttpClient httpClient;
    private final Gson gson;

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Generate embeddings for a given text using simple local hashing approach
     * (Fallback since Gemini Embedding API is not accessible)
     */
    public float[] generateEmbedding(String text) throws IOException {
        // Use simple text hashing to create 768-dimensional embeddings
        int dimension = 768;
        float[] embedding = new float[dimension];
        
        // Normalize and tokenize text
        String normalizedText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] words = normalizedText.split("\\s+");
        
        // Create embedding using word hashing
        for (String word : words) {
            if (word.length() > 0) {
                int hash = word.hashCode();
                int index = Math.abs(hash % dimension);
                embedding[index] += 1.0f;
            }
        }
        
        // Normalize the embedding vector (L2 normalization)
        float magnitude = 0.0f;
        for (float val : embedding) {
            magnitude += val * val;
        }
        magnitude = (float) Math.sqrt(magnitude);
        
        if (magnitude > 0) {
            for (int i = 0; i < dimension; i++) {
                embedding[i] /= magnitude;
            }
        }
        
        log.debug("Generated local embedding with dimension: {}", embedding.length);
        return embedding;
    }

    /**
     * Generate text response using Gemini API with RAG context
     */
    public String generateResponse(String context, String question) throws IOException {
        if (context == null || context.trim().isEmpty()) {
            return "I couldn't find relevant information in the knowledge base.";
        }
        
        // Always try Gemini API for intelligent answers
        try {
            log.info("Calling {} API for question: {}", llmProvider, question);
            String answer;
            
            if ("huggingface".equalsIgnoreCase(llmProvider)) {
                answer = callHuggingFaceAPI(context, question);
            } else {
                answer = callOllamaAPI(context, question);
            }
            
            log.info("{} API response: {}", llmProvider, answer.substring(0, Math.min(100, answer.length())));
            return answer;
        } catch (Exception e) {
            log.error("{} API failed: {}", llmProvider, e.getMessage());
            log.info("Using fallback summarization");
            // Return context as fallback
            String fallbackAnswer = summarizeContext(context);
            log.info("Fallback answer: {}", fallbackAnswer.substring(0, Math.min(100, fallbackAnswer.length())));
            return fallbackAnswer;
        }
    }
    
    private String callOllamaAPI(String context, String question) throws IOException {
        // Ollama API endpoint (local)
        String url = ollamaApiUrl;

        // Concise prompt for Ollama
        String prompt = String.format(
            "Based on the following context, answer the question directly and concisely. " +
            "Extract only the specific information requested. " +
            "Do not repeat the question or provide unnecessary details.\n\n" +
            "Context: %s\n\n" +
            "Question: %s\n\n" +
            "Answer:",
            context, question
        );

        // Ollama API request format
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", ollamaModelName);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);

        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                log.error("Ollama API error: {}", responseBody);
                throw new IOException("Ollama API returned error: " + response.code());
            }

            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Ollama returns response in "response" field
            String generatedText = jsonResponse.get("response").getAsString();

            return generatedText.trim();
        }
    }
    
    private String callHuggingFaceAPI(String context, String question) throws IOException {
        // Hugging Face Inference API endpoint
        String url = huggingfaceApiUrl + "/" + huggingfaceModelName;

        log.info("Calling Hugging Face API at: {}", url);
        log.info("Using model: {}", huggingfaceModelName);
        log.info("API Key present: {}", huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty());

        // Format for Phi-3 model (Microsoft's small but powerful LLM)
        String prompt = String.format(
            "<|user|>\nBased on this context, answer the question concisely.\n\nContext: %s\n\nQuestion: %s<|end|>\n<|assistant|>",
            context.length() > 800 ? context.substring(0, 800) : context,
            question
        );

        // Hugging Face API request format
        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_new_tokens", 200);
        parameters.addProperty("temperature", 0.7);
        parameters.addProperty("return_full_text", false);
        parameters.addProperty("do_sample", true);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("inputs", prompt);
        requestBody.add("parameters", parameters);

        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + huggingfaceApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            log.info("HF API Response Status: {}", response.code());
            log.info("HF API Response Body: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            
            if (!response.isSuccessful()) {
                log.error("Hugging Face API error ({}): {}", response.code(), responseBody);
                throw new IOException("Hugging Face API returned error: " + response.code() + " - " + responseBody);
            }

            // Hugging Face returns array of responses
            JsonArray jsonResponse = gson.fromJson(responseBody, JsonArray.class);
            
            if (jsonResponse != null && jsonResponse.size() > 0) {
                JsonObject firstResult = jsonResponse.get(0).getAsJsonObject();
                String generatedText = firstResult.get("generated_text").getAsString();
                log.info("Generated answer: {}", generatedText);
                return generatedText.trim();
            }
            
            throw new IOException("Empty response from Hugging Face API");
        }
    }
    
    private String summarizeContext(String context) {
        // Improved fallback: Return relevant context snippets instead of generic message
        log.warn("Using fallback summarization - HF API unavailable");
        
        if (context == null || context.trim().isEmpty()) {
            return "I couldn't find relevant information in the knowledge base.";
        }
        
        // Return first 500 characters of context as a basic answer
        String summary = context.trim();
        if (summary.length() > 500) {
            summary = summary.substring(0, 500) + "...";
        }
        
        return "Based on the documents: " + summary + "\n\n(Note: AI summarization is currently unavailable. Showing raw context.)";
    }
    
    private String extractCleanSentence(String line, String keyword) {
        // Clean up the line and try to extract relevant part
        String cleaned = line.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[•\\|]", "")
                .trim();
        
        // If line is short enough, return it
        if (cleaned.length() <= 150) {
            return cleaned;
        }
        
        // Try to find the sentence containing the keyword
        String[] parts = cleaned.split("[;\\|]");
        for (String part : parts) {
            if (part.toLowerCase().contains(keyword)) {
                return part.trim();
            }
        }
        
        // Fall back to first 150 chars
        return cleaned.substring(0, Math.min(150, cleaned.length())) + "...";
    }

    /**
     * Convert float array to PostgreSQL vector format string
     */
    public String floatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
