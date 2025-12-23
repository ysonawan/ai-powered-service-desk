package com.company.ai.help.desk.integrations;

import com.company.ai.help.desk.integrations.embedding.EmbeddingApiClient;
import com.company.ai.help.desk.service.KnowledgeBaseService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration service to automatically store JIRA tickets in the knowledge base.
 * This is an example of how to integrate RAG with existing ticket systems.
 */
@Service
@Slf4j
@AllArgsConstructor
public class JiraTicketEmbeddingService {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * Process a closed JIRA ticket and store it in the knowledge base
     *
     * @param ticketKey JIRA ticket key (e.g., "PROJ-123")
     * @param summary Ticket summary
     * @param description Ticket description
     * @param comments Ticket comments/resolution
     * @param assignee Assignee of the ticket
     * @param tenantId Tenant identifier (e.g., "jenkins")
     * @param projectKey Project key (e.g., "PROJ")
     * @param requestType Request type/category
     * @param priority Priority level (e.g., "High", "Medium", "Low")
     * @param status Ticket status (e.g., "Done", "Resolved", "Closed")
     * @throws EmbeddingApiClient.EmbeddingException if embedding fails
     */
    public void processClosedTicket(String ticketKey, String summary, String description,
                                    String comments, String assignee, String tenantId,
                                    String projectKey, String requestType, String priority, String status)
            throws EmbeddingApiClient.EmbeddingException {

        log.info("Processing JIRA ticket {} for tenant {} with status {} and priority {}",
                ticketKey, tenantId, status, priority);

        // Combine all content
        String fullContent = buildTicketContent(summary, description, comments);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectKey", projectKey);
        metadata.put("requestType", requestType);
        metadata.put("priority", priority);
        metadata.put("status", status);

        try {
            // Store in knowledge base
            knowledgeBaseService.storeTicket(
                    tenantId,
                    "JIRA",
                    ticketKey,
                    summary,
                    fullContent,
                    metadata
            );

            log.info("Successfully stored JIRA ticket {} in knowledge base", ticketKey);

        } catch (EmbeddingApiClient.EmbeddingException e) {
            log.error("Failed to embed JIRA ticket {}", ticketKey, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing JIRA ticket {}", ticketKey, e);
            throw new RuntimeException("Failed to process ticket", e);
        }
    }

    /**
     * Asynchronously process a JIRA ticket (non-blocking)
     * Can be used in event handlers for real-time updates
     */
    @Async
    public void processClosedTicketAsync(String ticketKey, String summary, String description,
                                        String comments, String assignee, String tenantId,
                                        String projectKey, String requestType, String priority, String status) {
        try {
            processClosedTicket(ticketKey, summary, description, comments, assignee, tenantId,
                    projectKey, requestType, priority, status);
        } catch (EmbeddingApiClient.EmbeddingException e) {
            log.error("Async ticket processing failed for {}", ticketKey, e);
        }
    }

    /**
     * Build the full content from ticket components
     */
    private String buildTicketContent(String summary, String description, String comments) {
        StringBuilder content = new StringBuilder();

        if (summary != null && !summary.isEmpty()) {
            content.append("SUMMARY: ").append(summary).append("\n\n");
        }

        if (description != null && !description.isEmpty()) {
            content.append("DESCRIPTION:\n").append(description).append("\n\n");
        }

        if (comments != null && !comments.isEmpty()) {
            content.append("COMMENTS AND RESOLUTION:\n").append(comments).append("\n");
        }

        return content.toString();
    }
}

