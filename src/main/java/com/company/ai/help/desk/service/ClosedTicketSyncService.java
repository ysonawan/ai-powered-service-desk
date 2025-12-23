package com.company.ai.help.desk.service;

import com.company.ai.help.desk.dto.Comments;
import com.company.ai.help.desk.dto.JiraSearchResult;
import com.company.ai.help.desk.integrations.JiraClient;
import com.company.ai.help.desk.integrations.JiraTicketEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service to synchronize closed tickets from Jira and embed them in the knowledge base.
 * Runs every 30 minutes to keep the knowledge base up-to-date with resolved tickets.
 */
@Service
@Slf4j
public class ClosedTicketSyncService {

    @Autowired
    private JiraClient jiraClient;

    @Autowired
    private JiraTicketEmbeddingService jiraTicketEmbeddingService;

    /**
     * Scheduled task to process closed tickets every scheduled minutes.
     * The initial delay is 1 minute to allow the application to start up properly.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void syncClosedTickets() {
        syncClosedTickets(null);
    }

    /**
     * Process closed tickets with a custom JQL query.
     *
     * @param jql The JQL query string, or null to use the default query
     */
    public void syncClosedTickets(String jql) {
        log.info("Starting scheduled sync of closed tickets");
        try {
            // Query for recently resolved/closed tickets using JQL
            // This searches for tickets that were resolved/closed in the last 30 minutes
            if(StringUtils.isEmpty(jql)) {
                 jql = "resolution is not EMPTY AND updated >= -30m ORDER BY updated DESC";
            }

            log.debug("Querying Jira for closed tickets using JQL: {}", jql);

            JiraSearchResult searchResult = jiraClient.searchTicketsByJql(jql, 100);

            if (searchResult == null || searchResult.issues().isEmpty()) {
                log.info("No closed tickets found in the last 30 minutes");
                return;
            }

            log.info("Found {} closed tickets to process", searchResult.issues().size());

            // Process each closed ticket
            for (JiraSearchResult.JiraIssue issue : searchResult.issues()) {
                try {
                    String ticketKey = issue.key();
                    JiraSearchResult.Fields fields = issue.fields();
                    String summary = fields.summary();
                    String description = fields.getDescriptionText() != null ?
                            fields.getDescriptionText() : "";

                    // Fetch comments for the ticket
                    String comments = "";
                    try {
                        Comments commentsResponse = jiraClient.getServiceDeskTicketComments(ticketKey);
                        if (commentsResponse != null && !commentsResponse.values().isEmpty()) {
                            StringBuilder commentsBuilder = new StringBuilder();
                            for (Comments.Comment comment : commentsResponse.values()) {
                                commentsBuilder.append(comment.body())
                                        .append("\n---\n");
                            }
                            comments = commentsBuilder.toString();
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch comments for ticket {}", ticketKey, e);
                    }

                    String assignee = fields.assignee() != null ?
                            fields.assignee().displayName() : "Unassigned";

                    // Extract project key for tenant ID
                    String projectKey = ticketKey.split("-")[0];
                    String tenantId = extractTenantFromProject(projectKey);

                    // Extract status
                    String status = fields.status() != null ?
                            fields.status().name() : "Unknown";

                    // Extract priority
                    String priority = fields.priority() != null ?
                            fields.priority().name() : "Unknown";

                    // Extract request type (from custom field or default)
                    String requestType = "General"; // Default value

                    jiraTicketEmbeddingService.processClosedTicketAsync(
                            ticketKey,
                            summary,
                            description,
                            comments,
                            assignee,
                            tenantId,
                            projectKey,
                            requestType,
                            priority,
                            status
                    );

                    log.debug("Queued ticket {} for embedding", ticketKey);

                } catch (Exception e) {
                    log.error("Error processing ticket during sync", e);
                }
            }

            log.info("Completed scheduled sync of closed tickets - queued {} tickets for processing",
                    searchResult.issues().size());

        } catch (Exception e) {
            log.error("Error during scheduled closed ticket sync", e);
        }
    }

    /**
     * Extract tenant ID from Jira project key.
     * Maps project keys to tenant IDs (e.g., "PROJ" -> "proj")
     *
     * @param projectKey The Jira project key
     * @return Tenant ID (lowercase project key)
     */
    private String extractTenantFromProject(String projectKey) {
        if (projectKey == null || projectKey.isEmpty()) {
            return "default";
        }
        return projectKey.toLowerCase();
    }
}
