package com.company.ai.help.desk.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpDeskPromptProvider {

    @McpPrompt(name = "service-desk-agent", description = "System prompt for Service Desk behavior")
    String serviceDeskAgentPrompt() {
        return """
            You are a Service Desk Agent. Response to user issues and errors using mcp tools to manage support tickets.
            Follow these rules:
            - Always fetch the list of available service desks using getServiceDesks tool before categorizing issues
            - Always search historical tickets before creating a new one using retrieveServiceDeskKnowledge tool
            - Never create duplicate tickets
            - If a solution is found in historical tickets in service desk knowledge, provide it to the user
            - If no solution is found, seek user confirmation before creating a new support ticket. Upon confirmation, create the ticket with all relevant details and always share the ticket link for reference.           
            - Ask clarifying questions if information is missing
            - Use MCP tools for all ticket operations
        """;
    }

    @McpPrompt(name = "list-service-desks", description = "List all service desks available to the user")
    public McpSchema.GetPromptResult listServiceDesks() {
        String message = "Please provide a list of all service desks available to the user.";
        return new McpSchema.GetPromptResult(
            "ListServiceDesks",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "report-issue", description = "Handle a user's issue or error by searching historical tickets and creating a new one if needed")
    public McpSchema.GetPromptResult reportIssue(
            @McpArg(name = "issueDescription", description = "Detailed description of the issue or error", required = true)
            String issueDescription) {
        String message = "I need help with the following issue: " + issueDescription + "\n\n" +
                "Please:\n" +
                "1. Determine the appropriate service desk category for this issue\n" +
                "2. Search historical tickets to find similar issues that may have been resolved using retrieveServiceDeskKnowledge tool\n" +
                "3. If similar solutions are found, provide recommendations based on those tickets\n" +
                "4. If no suitable solution is found, seek user confirmation before creating a new support ticket. Upon confirmation, create the ticket with all relevant details and always share the ticket link for reference.\n" +
                "5. Provide next steps and expected resolution time";
        return new McpSchema.GetPromptResult(
            "ReportIssue",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "search-similar-tickets", description = "Search for similar historical tickets to find solutions")
    public McpSchema.GetPromptResult searchSimilarTickets(
            @McpArg(name = "errorOrQuestion", description = "The error message or question to search for", required = true)
            String errorOrQuestion) {
        String message = "Please search for historical support tickets using retrieveServiceDeskKnowledge tool related to: " + errorOrQuestion + "\n";
        message += "\nFor each ticket found, provide:\n" +
                "- Ticket ID and summary\n" +
                "- Status and resolution\n" +
                "- Key comments explaining the solution\n" +
                "- How it relates to the current issue";
        return new McpSchema.GetPromptResult(
            "SearchSimilarTickets",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "ticket-details", description = "Get detailed information about a specific support ticket")
    public McpSchema.GetPromptResult getTicketDetails(
            @McpArg(name = "ticketId", description = "The ID of the ticket to fetch details for", required = true)
            String ticketId) {
        String message = "Please retrieve all details for support ticket: " + ticketId + "\n\n" +
                "Include:\n" +
                "- Full ticket description and status\n" +
                "- All comments and discussion history\n" +
                "- Current resolution or proposed solution\n" +
                "- Any attachments or related information\n" +
                "- Timeline of ticket lifecycle\n" +
                "- Provide ticket link for reference";
        return new McpSchema.GetPromptResult(
            "TicketDetails",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "create-support-ticket", description = "Create a new support ticket after searching for existing solutions")
    public McpSchema.GetPromptResult createSupportTicket(
            @McpArg(name = "summary", description = "Brief summary of the issue", required = true)
            String summary,
            @McpArg(name = "description", description = "Detailed description of the issue", required = true)
            String description) {
        String message = "I would like to create a new support ticket with the following information:\n\n" +
                "Summary: " + summary + "\n" +
                "Description: " + description + "\n\n" +
                "Please:\n" +
                "1. Identify the appropriate service desk category\n" +
                "2. Determine the correct request type\n" +
                "3. Create the ticket with all relevant details\n" +
                "4. Provide ticket link for reference and next steps";
        return new McpSchema.GetPromptResult(
            "CreateSupportTicket",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "resolve-ticket", description = "Resolve a support ticket after finding a solution")
    public McpSchema.GetPromptResult resolveTicket(
            @McpArg(name = "ticketId", description = "The ID of the ticket to resolve", required = true)
            String ticketId,
            @McpArg(name = "solution", description = "Summary of the solution applied", required = true)
            String solution) {
        String message = "Please resolve support ticket: " + ticketId + "\n\n" +
                "Solution Applied: " + solution + "\n\n" +
                "Steps to complete:\n" +
                "1. Add a final comment summarizing the solution\n" +
                "2. Transition the ticket to resolved state\n" +
                "3. Request user feedback on the solution quality\n" +
                "4. Provide closing comments and any follow-up actions";
        return new McpSchema.GetPromptResult(
            "ResolveTicket",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "add-ticket-comment", description = "Add a comment or update to an existing support ticket")
    public McpSchema.GetPromptResult addTicketComment(
            @McpArg(name = "ticketId", description = "The ID of the ticket to comment on", required = true)
            String ticketId,
            @McpArg(name = "comment", description = "The comment or update to add", required = true)
            String comment) {
        String message = "Please add the following comment to support ticket: " + ticketId + "\n\n" +
                "Comment: " + comment + "\n\n" +
                "This comment will be visible to the ticket creator and other stakeholders. " +
                "Please ensure the comment is clear, helpful, and provides any necessary updates or next steps.";
        return new McpSchema.GetPromptResult(
            "AddTicketComment",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    @McpPrompt(name = "rate-solution", description = "Rate the helpfulness of a solution for a resolved ticket")
    public McpSchema.GetPromptResult rateSolution(
            @McpArg(name = "ticketId", description = "The ID of the ticket to rate", required = true)
            String ticketId,
            @McpArg(name = "rating", description = "Rating from 1-5 where 5 is most helpful", required = true)
            String rating) {
        String message = "Please rate the solution for support ticket: " + ticketId + "\n\n" +
                "Rating: " + rating + " out of 5\n\n" +
                "Your feedback is valuable and will help improve our support process. " +
                "Please also provide any additional comments about the solution's effectiveness.";
        return new McpSchema.GetPromptResult(
            "RateSolution",
            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }
}
