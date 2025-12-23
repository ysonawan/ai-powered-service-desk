package com.company.ai.help.desk.service;

import com.company.ai.help.desk.entity.KnowledgeChunk;
import com.company.ai.help.desk.integrations.embedding.EmbeddingApiClient;
import com.company.ai.help.desk.integrations.embedding.TextCleaningService;
import com.company.ai.help.desk.repository.KnowledgeChunkRepository;
import com.pgvector.PGvector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service to manage knowledge chunks and embeddings for RAG.
 * Handles cleaning, normalization, embedding generation, and storage.
 */
@Service
@Slf4j
@AllArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final EmbeddingApiClient embeddingApiClient;
    private final TextCleaningService textCleaningService;

    private static final int CHUNK_SIZE = 1000; // characters
    private static final int CHUNK_OVERLAP = 200; // characters
    private static final int MIN_CHUNK_LENGTH = 10; // minimum length for a chunk
    private static final int MAX_CHUNK_LENGTH = 12000; // maximum length for a chunk

    /**
     * Store a document from a ticket with cleaning, chunking, and embedding
     *
     * @param tenantId Tenant identifier
     * @param sourceType Source type (JIRA, CONFLUENCE)
     * @param sourceId Ticket ID or page ID
     * @param sourceTitle Ticket summary or page title
     * @param content Full ticket content
     * @param metadata Additional metadata
     * @throws EmbeddingApiClient.EmbeddingException if embedding fails
     */
    @Transactional
    public void storeTicket(String tenantId, String sourceType, String sourceId,
                            String sourceTitle, String content, Map<String, Object> metadata)
            throws EmbeddingApiClient.EmbeddingException {

        log.info("Processing ticket: {} from {} ({})", sourceId, sourceType, tenantId);

        // Clean text
        String cleanedContent = textCleaningService.cleanText(content);

        // Validate content
        if (!textCleaningService.isValidForEmbedding(cleanedContent, MIN_CHUNK_LENGTH, MAX_CHUNK_LENGTH)) {
            log.warn("Ticket content is too short or invalid for embedding: {}", sourceId);
            return;
        }

        // Split into chunks
        String[] chunks = textCleaningService.splitIntoChunks(cleanedContent, CHUNK_SIZE, CHUNK_OVERLAP);

        if (chunks.length == 0) {
            log.warn("No valid chunks created for ticket: {}", sourceId);
            return;
        }

        log.info("Created {} chunks for ticket: {}", chunks.length, sourceId);

        // Delete existing chunks for this source (update scenario)
        knowledgeChunkRepository.deleteBySourceId(sourceId);

        // Process each chunk
        List<KnowledgeChunk> knowledgeChunks = new ArrayList<>();
        for (int i = 0; i < chunks.length; i++) {
            String chunkText = chunks[i];

            // Skip very short chunks
            if (chunkText.length() < MIN_CHUNK_LENGTH) {
                continue;
            }

            try {
                // Get embedding from API
                float[] embedding = embeddingApiClient.getEmbedding(chunkText);

                // Create PGvector from float array
                PGvector pgVector = new PGvector(embedding);

                // Create knowledge chunk
                Map<String, Object> chunkMetadata = new HashMap<>(metadata != null ? metadata : Map.of());
                KnowledgeChunk chunk = KnowledgeChunk.builder()
                        .tenantId(tenantId)
                        .sourceType(sourceType)
                        .sourceId(sourceId)
                        .sourceTitle(sourceTitle)
                        .content(chunkText)
                        .embedding(pgVector)
                        .metadata(chunkMetadata)
                        .build();

                knowledgeChunks.add(chunk);
                log.debug("Chunk {} processed for source: {}", i, sourceId);

            } catch (EmbeddingApiClient.EmbeddingException e) {
                log.error("Failed to get embedding for chunk {} of {}", i, sourceId, e);
                throw e;
            }
        }

        // Save all chunks
        if (!knowledgeChunks.isEmpty()) {
            knowledgeChunkRepository.saveAll(knowledgeChunks);
            log.info("Stored {} knowledge chunks for ticket: {}", knowledgeChunks.size(), sourceId);
        }
    }

    /**
     * Search for similar knowledge chunks using vector similarity
     *
     * @param tenantId Tenant identifier
     * @param query Query text
     * @param limit Maximum number of results
     * @return List of similar knowledge chunks
     * @throws EmbeddingApiClient.EmbeddingException if embedding fails
     */
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> search(String tenantId, String query, int limit)
            throws EmbeddingApiClient.EmbeddingException {

        log.info("Searching for similar chunks in tenant: {}", tenantId);

        // Clean and embed the query
        String cleanedQuery = textCleaningService.cleanText(query);
        if (cleanedQuery.length() < MIN_CHUNK_LENGTH) {
            log.warn("Query is too short after cleaning: {}", query);
            return Collections.emptyList();
        }

        float[] queryEmbedding = embeddingApiClient.getEmbedding(cleanedQuery);

        // Convert float array to pgvector format string
        String embeddingString = formatEmbeddingForQuery(queryEmbedding);

        // Search using vector similarity
        List<KnowledgeChunk> results = knowledgeChunkRepository.findSimilarChunks(
                tenantId.toLowerCase(), embeddingString, limit);

        log.info("Found {} similar chunks for query in tenant: {}", results.size(), tenantId);
        return results;
    }

    /**
     * Search with similarity scores
     *
     * @param tenantId Tenant identifier
     * @param query Query text
     * @param limit Maximum number of results
     * @param thresholdScore Minimum similarity score (0-1, where 0.5 = cosine distance 0.5)
     * @return List of search results with metadata
     * @throws EmbeddingApiClient.EmbeddingException if embedding fails
     */
    @Transactional(readOnly = true)
    public List<KnowledgeSearchResult> searchWithScores(String tenantId, String query, int limit, double thresholdScore)
            throws EmbeddingApiClient.EmbeddingException {

        // Clean and embed the query
        String cleanedQuery = textCleaningService.cleanText(query);
        if (cleanedQuery.length() < MIN_CHUNK_LENGTH) {
            return Collections.emptyList();
        }

        float[] queryEmbedding = embeddingApiClient.getEmbedding(cleanedQuery);
        String embeddingString = formatEmbeddingForQuery(queryEmbedding);

        // Get results with distance
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findSimilarChunks(tenantId.toLowerCase(), embeddingString, limit);

        // Convert distance to similarity score (cosine similarity = 1 - cosine distance)
        return chunks.stream()
                .map(chunk -> new KnowledgeSearchResult(
                        chunk,
                        1.0 - this.getChunkDistance(embeddingString, chunk), // Convert distance to similarity
                        tenantId
                ))
                .filter(result -> result.getSimilarityScore() >= thresholdScore)
                .toList();
    }

    private double getChunkDistance(String embeddingString, KnowledgeChunk chunk) {
        return knowledgeChunkRepository.getEmbeddingDistance(embeddingString, chunk.getId());
    }

    /**
     * Remove knowledge chunks for a ticket
     *
     * @param sourceId Ticket ID to remove
     * @return Number of chunks deleted
     */
    @Transactional
    public long removeTicket(String sourceId) {
        log.info("Removing chunks for source: {}", sourceId);
        long deletedCount = knowledgeChunkRepository.deleteBySourceId(sourceId);
        log.info("Deleted {} chunks for source: {}", deletedCount, sourceId);
        return deletedCount;
    }

    private String formatEmbeddingForQuery(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Data
    public static class KnowledgeSearchResult {
        private final KnowledgeChunk chunk;
        private final double similarityScore;
        private final String tenantId;

        public KnowledgeSearchResult(KnowledgeChunk chunk, double similarityScore, String tenantId) {
            this.chunk = chunk;
            this.similarityScore = Math.max(0, Math.min(1, similarityScore)); // Clamp between 0 and 1
            this.tenantId = tenantId;
        }
    }
}
