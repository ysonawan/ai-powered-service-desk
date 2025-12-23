package com.company.ai.help.desk.integrations;

import com.company.ai.help.desk.dto.ConfluenceDocument;
import com.company.ai.help.desk.integrations.embedding.EmbeddingApiClient;
import com.company.ai.help.desk.service.KnowledgeBaseService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration service to automatically store Confluence documents in the knowledge base.
 * Handles extraction and embedding of Confluence pages for RAG integration.
 */
@Service
@Slf4j
public class ConfluenceEmbeddingService {

    private final KnowledgeBaseService knowledgeBaseService;

    public ConfluenceEmbeddingService(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Configuration object for processing Confluence documents
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class ConfluenceDocConfig {
        private String pageId;
        private String title;
        private String content;
        private String spaceId;
        private String authorId;
        private String createdAt;
        private String tenantId;
        private String webUrl;
    }

    /**
     * Process a Confluence document and store it in the knowledge base
     *
     * @param config The Confluence document configuration object
     * @throws EmbeddingApiClient.EmbeddingException if embedding fails
     */
    public void processConfluenceDocument(ConfluenceDocConfig config)
            throws EmbeddingApiClient.EmbeddingException {

        log.info("Processing Confluence document {} for tenant {} with title: {}",
                config.pageId, config.tenantId, config.title);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("spaceId", config.spaceId);
        metadata.put("authorId", config.authorId);
        metadata.put("createdAt", config.createdAt);
        metadata.put("webUrl", config.webUrl);

        try {
            // Store in knowledge base
            knowledgeBaseService.storeTicket(
                    config.tenantId,
                    "CONFLUENCE",
                    config.pageId,
                    config.title,
                    config.content,
                    metadata
            );

            log.info("Successfully stored Confluence document {} in knowledge base", config.pageId);

        } catch (EmbeddingApiClient.EmbeddingException e) {
            log.error("Failed to embed Confluence document {}", config.pageId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing Confluence document {}", config.pageId, e);
            throw new EmbeddingApiClient.EmbeddingException("Failed to process Confluence document", e);
        }
    }

    /**
     * Asynchronously process a Confluence document (non-blocking)
     * Can be used in scheduled tasks or event handlers
     */
    @Async
    public void processConfluenceDocumentAsync(ConfluenceDocConfig config) {
        try {
            processConfluenceDocument(config);
        } catch (EmbeddingApiClient.EmbeddingException e) {
            log.error("Async Confluence document processing failed for {}", config.pageId, e);
        }
    }
}

