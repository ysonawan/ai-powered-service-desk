package com.company.ai.help.desk.resources;

import com.company.ai.help.desk.dto.ServiceDesk;
import com.company.ai.help.desk.integrations.JiraClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpDeskResourceProvider {

    private final JiraClient jiraClient;
    private final ObjectMapper objectMapper;

    @McpResource(uri = "servicedesk://projects",
            description = "List of available service desk projects and categories")
    public String getServiceDesks() {
        try {
            ServiceDesk serviceDesks = jiraClient.getServiceDesks();
            return objectMapper.writeValueAsString(serviceDesks);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize service desks", e);
        }
    }

    @McpResource(uri = "policy://handle-request", description = "Rules for handling service desk requests for question or error")
    String policy() {
        return """
            - Always fetch the list of available service desks using getServiceDesks tool before categorizing issues
            - Always search historical tickets before creating a new one using retrieveServiceDeskKnowledge tool
            - Never create duplicate tickets
            - If a solution is found in historical tickets in service desk knowledge, provide it to the user
            - If no solution is found, seek user confirmation before creating a new support ticket. Upon confirmation, create the ticket with all relevant details and always share the ticket link for reference.           
            - Ask clarifying questions if information is missing
            - Use MCP tools for all ticket operations
        """;
    }
}
