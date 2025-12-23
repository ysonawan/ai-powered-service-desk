package com.company.ai.help.desk.service;

import com.company.ai.help.desk.dto.ConfluenceDocument;
import com.company.ai.help.desk.integrations.ConfluenceClient;
import com.company.ai.help.desk.integrations.ConfluenceEmbeddingService;
import com.company.ai.help.desk.repository.KnowledgeChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to synchronize Confluence documents and embed them in the knowledge base.
 * Fetches documents from configured Confluence spaces and stores them in pgvector DB.
 * Runs on application startup and embeds documents if not already present.
 *
 * Configuration required in application.yml:
 *   confluence:
 *     url: https://help-desk-mcp-demo.atlassian.net/wiki
 *     bearer-token: your-bearer-token
 *     documents:
 *       - page-id: "1015813"
 *         tenant-id: "bitsup"
 *       - page-id: "1277953"
 *         tenant-id: "sonarsup"
 */
@Service
@Slf4j
public class ConfluenceDocumentSyncService {

    private final ConfluenceClient confluenceClient;
    private final ConfluenceEmbeddingService confluenceEmbeddingService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final String bitsupPageId;
    private final String sonarsupPageId;

    public ConfluenceDocumentSyncService(
            ConfluenceClient confluenceClient,
            ConfluenceEmbeddingService confluenceEmbeddingService,
            KnowledgeChunkRepository knowledgeChunkRepository,
            @Value("${confluence.documents[0].page-id:1015813}") String bitsupPageId,
            @Value("${confluence.documents[1].page-id:1277953}") String sonarsupPageId) {
        this.confluenceClient = confluenceClient;
        this.confluenceEmbeddingService = confluenceEmbeddingService;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.bitsupPageId = bitsupPageId;
        this.sonarsupPageId = sonarsupPageId;
    }

    /**
     * Sync Confluence documents on application startup
     * Embeds only if the page ID is not already present in the knowledge base
     */
    public void syncConfluenceDocuments() {
        log.info("Starting Confluence documents sync");
        try {
            // Sync BitSup documentation
            syncConfluenceDocument(bitsupPageId, "bitsup");

            // Sync SonarSup documentation
            syncConfluenceDocument(sonarsupPageId, "sonarsup");

            log.info("Completed Confluence documents sync");

        } catch (Exception e) {
            log.error("Error during Confluence document sync", e);
        }
    }

    /**
     * Sync a single Confluence document by its page ID
     * Only embeds if the document is not already in the knowledge base
     *
     * @param pageId The Confluence page ID
     * @param tenantId The tenant identifier (e.g., "bitsup", "sonarsup")
     */
    public void syncConfluenceDocument(String pageId, String tenantId) {
        try {
            log.info("Syncing Confluence document {} for tenant {}", pageId, tenantId);

            // Check if document already exists in knowledge base
            boolean exists = knowledgeChunkRepository.existsBySourceIdAndSourceType(pageId, "CONFLUENCE");
            if (exists) {
                log.info("Confluence document {} already exists in knowledge base. Skipping embedding", pageId);
                return;
            }

            // Fetch the document from Confluence
            ConfluenceDocument document = confluenceClient.getConfluenceDocument(pageId);

            if (document == null) {
                log.warn("Failed to fetch Confluence document {} for tenant {}", pageId, tenantId);
                return;
            }

            // Extract content from storage format
            String plainTextContent = extractContentFromDocument(document);

            if (plainTextContent.trim().isEmpty()) {
                log.warn("Confluence document {} has empty content", pageId);
                return;
            }

            // Build web URL
            String webUrl = buildWebUrl(document);

            // Build config and queue for embedding
            ConfluenceEmbeddingService.ConfluenceDocConfig config =
                    ConfluenceEmbeddingService.ConfluenceDocConfig.builder()
                            .pageId(document.id())
                            .title(document.title())
                            .content(plainTextContent)
                            .spaceId(document.spaceId())
                            .authorId(document.authorId())
                            .createdAt(document.createdAt())
                            .tenantId(tenantId)
                            .webUrl(webUrl)
                            .build();

            confluenceEmbeddingService.processConfluenceDocumentAsync(config);

            log.info("Queued Confluence document {} for embedding", pageId);

        } catch (Exception e) {
            log.error("Error syncing Confluence document {} for tenant {}", pageId, tenantId, e);
        }
    }

    /**
     * Extract content from Confluence document
     * Combines title and plain text from storage body
     *
     * @param document The Confluence document
     * @return Plain text content
     */
    private String extractContentFromDocument(ConfluenceDocument document) {
        StringBuilder content = new StringBuilder();

        // Add title
        if (document.title() != null && !document.title().isEmpty()) {
            content.append("Title: ").append(document.title()).append("\n\n");
        }

        // Extract and add body content
        if (document.body() != null &&
            document.body().storage() != null &&
            document.body().storage().value() != null) {

            String plainText = confluenceClient.extractPlainText(
                    document.body().storage().value()
            );
            content.append(plainText);
        }

        return content.toString();
    }

    /**
     * Build the web URL for the Confluence document
     *
     * @param document The Confluence document
     * @return Full web URL
     */
    private String buildWebUrl(ConfluenceDocument document) {
        if (document.links() != null && document.links().base() != null) {
            String base = document.links().base();
            String webui = document.links().webui() != null ?
                    document.links().webui() : "";
            return base + webui;
        }
        return "";
    }

    /**
     * Manually trigger sync of a specific Confluence document
     * Useful for testing or manual refresh
     *
     * @param pageId The Confluence page ID
     * @param tenantId The tenant identifier
     */
    public void manualSyncConfluenceDocument(String pageId, String tenantId) {
        log.info("Manual sync triggered for Confluence document {} and tenant {}", pageId, tenantId);
        syncConfluenceDocument(pageId, tenantId);
    }
}

