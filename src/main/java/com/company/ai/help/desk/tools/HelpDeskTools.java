package com.company.ai.help.desk.tools;

import com.company.ai.help.desk.integrations.JiraClient;
import com.company.ai.help.desk.dto.*;
import com.company.ai.help.desk.integrations.embedding.EmbeddingApiClient;
import com.company.ai.help.desk.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.model.source.spi.IdentifierSource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Help Desk MCP Service
 *
 * Provides tools for:
 * 1. Error reporting and analysis
 * 2. Searching historical tickets in Jira
 * 3. Suggesting solutions based on historical data
 * 4. Creating support tickets in appropriate Jira projects
 * 5. Categorizing issues to the right support category
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpDeskTools {

    private final JiraClient jiraClient;
    private final KnowledgeBaseService knowledgeBaseService;

    // ===================== MCP Tools =====================

    /**
     * list of all service desks
     */

    @McpTool(name = "retrieveServiceDeskKnowledge",
            description = "Retrieve relevant historical tickets, KB articles for the reported question or issue." +
    "The AI model should use this tool to find solutions from historical tickets before creating a new service desk ticket. AI model should provide tenant id, query, limit and threshold score.")
    public List<KnowledgeBaseService.KnowledgeSearchResult> retrieveServiceDeskKnowledge(
            @McpToolParam(description = "Tenant Id is service desk project key") String tenantId,
            @McpToolParam(description = "Question or error asked by the user" ) String query,
            @McpToolParam(description = "Number of results from knowledge base") int limit,
            @McpToolParam(description = "Threshold score for the match") double thresholdScore)
            throws EmbeddingApiClient.EmbeddingException {
        List<KnowledgeBaseService.KnowledgeSearchResult> knowledgeSearchResults = knowledgeBaseService.searchWithScores(tenantId, query, limit, thresholdScore);
        //remove embedding from results
        knowledgeSearchResults.forEach(result -> result.getChunk().setEmbedding(null));
        return knowledgeSearchResults;
    }

    @McpTool(name = "getServiceDesks",
            description = "Retrieves the list of supported Jira Service Management service desk categories for internal systems. "+
                    "The AI model uses this information to select the appropriate service desk when creating a service desk tickets.")
    public ServiceDeskResponse getServiceDesks() {
        log.info("Fetching list of service desks");
        try {
            ServiceDesk serviceDesk = jiraClient.getServiceDesks();
            log.info("Retrieved {} service desks", serviceDesk.size());
            return new ServiceDeskResponse(
                    "Success",
                    "Retrieved list of service desks",
                    serviceDesk);
        } catch (Exception e) {
            log.error("Error fetching service desks", e);
            return new ServiceDeskResponse(
                    "Error",
                    "Failed to retrieve list of service desks",
                    null);        }
    }

    @McpTool(name = "getServiceDeskRequestTypes",
            description = "Retrieves the list of request types for a service desk category. The AI model should select the appropriate service desk id for the users request. "+
                    "The AI model uses this information to select the appropriate request type when creating a service desk tickets.")
    public RequestTypesResponse getServiceDeskRequestTypes(@McpToolParam(description = "Service desk project id") String serviceDeskId) {
        log.info("Fetching list of service desk request types");
        try {
            RequestTypes serviceDeskRequestTypes = jiraClient.getServiceDeskRequestTypes(serviceDeskId);
            log.info("Retrieved {} service desk request types", serviceDeskRequestTypes.size());
            return new RequestTypesResponse(
                    "Success",
                    "Retrieved list of service desk request types",
                    serviceDeskRequestTypes);
        } catch (Exception e) {
            log.error("Error fetching service desk request types", e);
            return new RequestTypesResponse(
                    "Error",
                    "Failed to retrieve list of service desk request types",
                    null);        }
    }

    /**
     * Gets detailed information about a specific ticket
     */
    @McpTool(name = "getServiceDeskTicketDetails",
            description = "Get complete details of a service desk ticket including the full solution. " +
                    "This is useful for understanding how past issues were resolved.")
    public ServiceDeskTicketResponse getServiceDeskTicketDetails(@McpToolParam(description = "Service desk ticket id") String ticketId) {

        log.info("Fetching service desk ticket details for ticket: {}", ticketId);
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Ticket ID is required",
                    null);
        }
        try {
            ServiceDeskTicket ticket = jiraClient.getServiceDeskTicketDetails(ticketId);
            if (ticket == null) {
                return new ServiceDeskTicketResponse(
                        "NotFound",
                        "Service desk ticket not found: " + ticketId,
                        null);
            }
            return new ServiceDeskTicketResponse(
                    "Success",
                    "Retrieved service desk ticket details",
                    ticket);

        } catch (Exception e) {
            log.error("Error fetching solution details", e);
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Failed to fetch service desk ticket details: " + e.getMessage(),
                    null);
        }
    }

    /**
     * Adds a comment to an existing ticket
     */
    @McpTool(name = "addServiceDeskTicketComment",
            description = "Add a comment to an existing support ticket.")
    public AddCommentResultResponse addCommentToTicket(
            @McpToolParam(description = "Service desk ticket id") String ticketId,
            @McpToolParam(description = "Comment to be added in the ticket") String comment) {
        log.info("Adding comment to ticket: {}", ticketId);
        if (ticketId == null || ticketId.trim().isEmpty() ||
                comment == null || comment.trim().isEmpty()) {
            return new AddCommentResultResponse(
                    "Error",
                    "Ticket ID and comment are required",
                    null);
        }
        try {
            AddCommentResult addCommentResult = jiraClient.addServiceDeskTicketComment(ticketId, comment);
            return new AddCommentResultResponse(
                    "Success",
                    "Comment added successfully",
                    addCommentResult);

        } catch (Exception e) {
            log.error("Error adding comment", e);
            return new AddCommentResultResponse(
                    "Error",
                    "Failed to add comment: " + e.getMessage(),
                    null);
        }
    }

    /**
     * Gets comments for a specific ticket
     */
    @McpTool(name = "getServiceDeskTicketComments",
            description = "Get comments of a service desk ticket including internal and public comments to understand the ticket resolution. " +
                    "This is useful for understanding communication around past issues and the resolution for the ticket.")
    public CommentsResponse getServiceDeskTicketComments(@McpToolParam(description = "Service desk ticket id") String ticketId) {
        log.info("Fetching service desk ticket comments for ticket: {}", ticketId);
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return new CommentsResponse(
                    "Error",
                    "Ticket ID is required",
                    null);
        }
        try {
            Comments serviceDeskRequestComments = jiraClient.getServiceDeskTicketComments(ticketId);
            log.info("Retrieved {} comments for ticket: {}", serviceDeskRequestComments.size(), ticketId);
            return new CommentsResponse(
                    "Success",
                    "Retrieved service desk ticket comments",
                    serviceDeskRequestComments);

        } catch (Exception e) {
            log.error("Error fetching ticket comments", e);
            return new CommentsResponse(
                    "Error",
                    "Failed to fetch service desk ticket comments: " + e.getMessage(),
                    null);
        }
    }

    /**
     * Creates a new support ticket in the appropriate Jira project
     *
     * NOTE: The AI model should provide the category for the ticket.
     */
    @McpTool(name = "createServiceDeskTicket",
            description = "Create a new service desk ticket in the appropriate service desk project. Seek user confirmation before creating a new support ticket. Upon confirmation, create the ticket with all relevant details and always share the ticket link for reference." +
                    "The AI model should determine the correct service desk category, request type within that service desk category. " +
                    "AI model should also specify the error description, summary and other details.")
    public ServiceDeskTicketResponse createServiceDeskTicket(
            @McpToolParam(description = "Description to be added for the service desk ticket") String errorDescription,
            @McpToolParam(description = "Summary to be added for the service desk ticket") String summary,
            @McpToolParam(description = "Service desk project id") String serviceDeskId,
            @McpToolParam(description = "Request type id for the Service desk project") String requestTypeId) {
        if (errorDescription == null || errorDescription.trim().isEmpty()) {
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Error description is required",
                    null);
        }
        try {
            log.info("Creating service desk ticket in service desk: {}, request type: {}", serviceDeskId, requestTypeId);
            ServiceDeskTicket createdTicket = jiraClient.createServiceDeskTicket(
                    serviceDeskId,
                    requestTypeId,
                    summary,
                    errorDescription);

            if (createdTicket == null) {
                return new ServiceDeskTicketResponse(
                        "Error",
                        "Failed to create ticket - received null response from Jira",
                        null);
            }

            return new ServiceDeskTicketResponse(
                    "Success",
                    "Ticket created successfully in service desk",
                    createdTicket);

        } catch (Exception e) {
            log.error("Error creating support ticket", e);
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Failed to create ticket: " + e.getMessage(),
                    null);
        }
    }

    /**
     * Searches for historical tickets matching the query
     */
    //@McpTool(name = "searchHistoricalServiceDeskTickets",
    //        description = "Search historical service desk tickets using a bounded JQL query. " +
    //                "The tool enforces project key and time constraints for reliability. The AI model should provide the search query and the service desk project key."
    //)
    public JiraSearchResultResponse searchHistoricalServiceDeskTickets(
            @McpToolParam(description = "Search query") String query,
            @McpToolParam(description = "Service desk project key") String serviceDeskProject) {

        log.info("Searching historical tickets. query='{}', project='{}'", query, serviceDeskProject);

        if (query == null || query.isBlank()) {
            return new JiraSearchResultResponse("Error", "Query is required", null);
        }

        if (serviceDeskProject == null || serviceDeskProject.isBlank()) {
            return new JiraSearchResultResponse("Error", "Service desk project key is required", null);
        }

        try {
            String escapedQuery = query.replace("\"", "\\\"");
            String projectKey = serviceDeskProject.trim();

            String jql =
                    "project = \"" + projectKey + "\" " +
                            "AND updated >= -30d " +
                            "ORDER BY updated DESC";

            log.info("Executing JQL: {}", jql);

            JiraSearchResult result = jiraClient.searchTicketsByJql(jql, 50);

            if (result == null || result.issues() == null || result.issues().isEmpty()) {
                return new JiraSearchResultResponse(
                        "Success",
                        "No tickets found matching the query",
                        null
                );
            }

            return new JiraSearchResultResponse(
                    "Success",
                    "Found " + result.issues().size() + " ticket(s)",
                    result
            );

        } catch (Exception e) {
            log.error("Error searching tickets", e);
            return new JiraSearchResultResponse(
                    "Error",
                    "Search failed: " + e.getMessage(),
                    null
            );
        }
    }

    /**
     * Reports an error and searches for similar historical tickets with solutions
     *
     * NOTE: The AI model should classify the error and provide the category.
     * This tool will use the provided category to search for similar tickets.
     */
    //@McpTool(name = "handleServiceDeskRequest",
    //        description = "Handles all service desk requests including questions and errors. Generate AI-powered suggestions based on historical tickets. " +
    //                "The AI model should classify the error into one of the supported service desk categories and create ticket if no historical tickets found.")
    public ReportErrorResolutionResponse handleServiceDeskRequest(
            String errorDescription,
            String summary,
            @McpToolParam(description = "Service desk project id") String serviceDeskId,
            @McpToolParam(description = "Service desk project key") String serviceDeskProjectKey,
            @McpToolParam(description = "Request type id for the service desk project") String requestTypeId
    ) {
        // 1. Search tickets
        JiraSearchResultResponse jiraSearchResultResponse = searchHistoricalServiceDeskTickets(summary, serviceDeskProjectKey);

        if (null != jiraSearchResultResponse.jiraSearchResult()) {
            return new ReportErrorResolutionResponse(
                    "Success",
                    "Historical tickets found matching the query",
                    jiraSearchResultResponse.jiraSearchResult(), null
            );
        }

        // 2. Create ticket
        ServiceDeskTicketResponse serviceDeskTicketResponse = createServiceDeskTicket(errorDescription, summary, serviceDeskId, requestTypeId);
        return new ReportErrorResolutionResponse(
                "Success",
                "Ticket created successfully in service desk",
              null, serviceDeskTicketResponse.serviceDeskTicket()
        );
    }

    @McpTool(name = "resolveServiceDeskTicket",
            description = "Resolve a service desk ticket after user confirmation by transitioning it to a resolved state. ")
    public ServiceDeskTicketResponse resolveServiceDeskTicket(@McpToolParam(description = "Service desk ticket id") String serviceDeskTicketId) {
        log.info("Resolving service desk ticket: {}", serviceDeskTicketId);

        if (serviceDeskTicketId == null || serviceDeskTicketId.trim().isEmpty()) {
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Service desk ticket id is required",
                    null);
        }

        try {
            ServiceDeskTicket resolvedTicket = jiraClient.resolveServiceDeskTicket(serviceDeskTicketId);
            if (resolvedTicket == null) {
                return new ServiceDeskTicketResponse(
                        "Error",
                        "Service desk ticket not found: " + serviceDeskTicketId,
                        null);
            }
            return new ServiceDeskTicketResponse(
                    "Success",
                    "Service desk ticket resolved successfully",
                    resolvedTicket);

        } catch (Exception e) {
            log.error("Error resolving service desk ticket", e);
            return new ServiceDeskTicketResponse(
                    "Error",
                    "Failed to resolve service desk ticket: " + e.getMessage(),
                    null);
        }

    }
    /**
     * Rates the helpfulness of a solution
     */
    @McpTool(name = "rateServiceDeskTicketSolution",
            description = "Rate the helpfulness of a solution or ticket for the service desk ticket. The AI model should resolve the ticket using resolveServiceDeskTicket tool before rating the solution. "+
                    "The AI model should use this tool to provide feedback on whether the provided solution was helpful or not.")
    public RateSolutionResponse rateServiceDeskTicketSolution(
            @McpToolParam(description = "Service desk ticket id") String serviceDeskTicketId,
            @McpToolParam(description = "Rating for the ticket") int rating,
            @McpToolParam(description = "Feedback for the ticket") String feedback) {

        log.info("Rating ticket {} with rating: {}, feedback: {}", serviceDeskTicketId, rating, feedback);

        if (serviceDeskTicketId == null || serviceDeskTicketId.trim().isEmpty() || rating < 1 || rating > 5) {
            return new RateSolutionResponse(
                    "Error",
                    "Valid ticket ID and rating (1-5) are required",
                    false);
        }

        try {
            boolean status = jiraClient.updateRating(serviceDeskTicketId, rating, feedback);
            if(status) {
                return new RateSolutionResponse(
                        "Success",
                        "Thank you for your feedback!",
                        true);
            } else {
                return new RateSolutionResponse(
                        "Error",
                        "Failed to submit rating",
                        false);
            }
        } catch (Exception e) {
            log.error("Error rating solution", e);
            return new RateSolutionResponse(
                    "Error",
                    "Failed to submit rating: " + e.getMessage(),
                    false);
        }
    }
}
