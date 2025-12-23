package com.company.ai.help.desk.dto;

public record ReportErrorResolutionResponse(
        String status,
        String message,
        JiraSearchResult jiraSearchResult,
        ServiceDeskTicket serviceDeskTicket) {
}
