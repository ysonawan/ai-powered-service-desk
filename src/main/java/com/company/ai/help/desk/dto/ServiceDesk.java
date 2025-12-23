package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Bean representing the Jira Service Desk API response for listing all service desks.
 *
 * Corresponds to: GET /rest/servicedeskapi/servicedesk
 */
public record ServiceDesk(
        int size,
        int start,
        int limit,
        @JsonProperty("isLastPage")
        boolean isLastPage,
        @JsonProperty("_links") com.company.ai.help.desk.dto.ServiceDesk.Links links,
        List<com.company.ai.help.desk.dto.ServiceDesk.ServiceDeskDetails> values
) {
    /**
     * Represents a single Service Desk project
     */
    public record ServiceDeskDetails(
            String id,
            String projectId,
            String projectName,
            String projectKey,
            String projectTypeKey,
            @JsonProperty("_links")
            Map<String, Object> links
    ) {}

    /**
     * Represents the links section in the API response
     */
    public record Links(
            String self,
            String base,
            String context
    ) {}
}

