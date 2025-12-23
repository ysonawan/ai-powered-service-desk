package com.company.ai.help.desk.integrations.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Client to communicate with the local embedding model API.
 * Expects the API to support E5-base model (768 dimensions).
 */
@Component
@Slf4j
public class EmbeddingApiClient {

    @Value("${embedding.api.url:http://localhost:8001/embed}")
    private String apiUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Call the embedding API to get embeddings for the input text.
     *
     * @param text The text to embed
     * @return Array of floats representing the embedding (768 dimensions)
     * @throws EmbeddingException if the API call fails
     */
    public float[] getEmbedding(String text) throws EmbeddingException {
        if (text == null || text.trim().isEmpty()) {
            throw new EmbeddingException("Input text cannot be null or empty");
        }

        try {
            EmbeddingRequest request = new EmbeddingRequest(text);
            String requestBody = objectMapper.writeValueAsString(request);

            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Embedding API failed with status {}: {}", response.code(), errorBody);
                    throw new EmbeddingException("Embedding API returned status: " + response.code());
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new EmbeddingException("Empty response from embedding API");
                }

                String responseBody = body.string();
                EmbeddingResponseData responseData = objectMapper.readValue(responseBody, EmbeddingResponseData.class);

                if (responseData.getEmbedding() == null || responseData.getEmbedding().length == 0) {
                    throw new EmbeddingException("No embeddings returned from API");
                }

                return responseData.getEmbedding();
            }
        } catch (IOException e) {
            log.error("Error calling embedding API", e);
            throw new EmbeddingException("Failed to get embedding from API: " + e.getMessage(), e);
        }
    }

    /**
     * Batch request for embeddings
     *
     * @param texts List of texts to embed
     * @return List of embedding arrays
     * @throws EmbeddingException if the API call fails
     */
    public List<float[]> getEmbeddings(List<String> texts) throws EmbeddingException {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts list cannot be null or empty");
        }

        try {
            EmbeddingBatchRequest request = new EmbeddingBatchRequest(texts);
            String requestBody = objectMapper.writeValueAsString(request);

            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Embedding API failed with status {}: {}", response.code(), errorBody);
                    throw new EmbeddingException("Embedding API returned status: " + response.code());
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new EmbeddingException("Empty response from embedding API");
                }

                String responseBody = body.string();
                EmbeddingResponseData[] responsesArray = objectMapper.readValue(responseBody, EmbeddingResponseData[].class);

                return java.util.Arrays.stream(responsesArray)
                        .map(EmbeddingResponseData::getEmbedding)
                        .toList();
            }
        } catch (IOException e) {
            log.error("Error calling embedding batch API", e);
            throw new EmbeddingException("Failed to get embeddings from API: " + e.getMessage(), e);
        }
    }

    // ============ DTO Classes ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingRequest {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingBatchRequest {
        private List<String> texts;
    }

    @Data
    @NoArgsConstructor
    public static class EmbeddingResponseData {
        @JsonProperty("embedding")
        private float[] embedding;
        @JsonProperty("model")
        private String model;
        @JsonProperty("dimensions")
        private int dimensions;
    }

    /**
     * Custom exception for embedding API errors
     */
    public static class EmbeddingException extends Exception {
        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

