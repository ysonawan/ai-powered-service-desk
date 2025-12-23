package com.company.ai.help.desk.dto;

public record JiraSearchResultResponse(
        String status,
        String message,
        JiraSearchResult jiraSearchResult) {
}
