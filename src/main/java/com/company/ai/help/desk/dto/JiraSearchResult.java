package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO representing the Jira API search response
 * Corresponds to: GET /rest/api/3/search?jql={jql}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSearchResult(
        int startAt,
        int maxResults,
        int total,
        boolean isLast,
        List<JiraIssue> issues
) {
    /**
     * Represents a single Jira issue returned from search
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JiraIssue(
            String key,
            String id,
            @JsonProperty("self")
            String selfUrl,
            Fields fields
    ) {}

    /**
     * Represents the fields of a Jira issue
     * Note: description is ADF (Atlassian Document Format) in v3 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fields(
            String summary,
            Object description,  // ADF object in v3, use Object to avoid parsing issues
            Status status,
            String created,
            String updated,
            Priority priority,
            Assignee assignee
    ) {
        /**
         * Get description as plain text (extracts from ADF if needed)
         */
        public String getDescriptionText() {
            if (description == null) {
                return null;
            }
            if (description instanceof String) {
                return (String) description;
            }
            // For ADF content, return a simplified representation
            return description.toString();
        }
    }

    /**
     * Represents the status of an issue
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            String id,
            String name,
            StatusCategory statusCategory
    ) {}

    /**
     * Represents the status category
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusCategory(
            String id,
            String key,
            String name,
            String colorName
    ) {}

    /**
     * Represents the priority of an issue
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Priority(
            String id,
            String name
    ) {}

    /**
     * Represents the assignee of an issue
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assignee(
            String accountId,
            String displayName,
            String emailAddress
    ) {}
}

