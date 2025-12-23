package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bean representing the Jira Service Desk API response for listing all request types for a service desk.
 *
 * Corresponds to: GET /rest/servicedeskapi/servicedesk/{serviceDeskId}/requesttype
 */
public record RequestTypes(
        @JsonProperty("_expands")
        List<String> expands,
        int size,
        int start,
        int limit,
        @JsonProperty("isLastPage")
        boolean isLastPage,
        @JsonProperty("_links")
        Links links,
        List<RequestType> values
) {
    /**
     * Represents a single request type available on a service desk
     */
    public record RequestType(
            @JsonProperty("_expands")
            List<String> expands,
            String id,
            @JsonProperty("_links")
            RequestTypeLinks requestTypeLinks,
            String name,
            String description,
            String helpText,
            String defaultName,
            String issueTypeId,
            String serviceDeskId,
            String portalId,
            List<String> groupIds,
            Icon icon,
            String restrictionStatus,
            boolean canCreateRequest
    ) {}

    /**
     * Represents request type specific links
     */
    public record RequestTypeLinks(
            String self
    ) {}

    /**
     * Represents the icon for a request type
     */
    public record Icon(
            String id,
            @JsonProperty("_links")
            IconLinks iconLinks
    ) {}

    /**
     * Represents icon links with avatar URLs
     */
    public record IconLinks(
            @JsonProperty("iconUrls")
            IconUrls iconUrls
    ) {}

    /**
     * Represents avatar URLs in different sizes
     */
    public record IconUrls(
            @JsonProperty("48x48")
            String size48x48,
            @JsonProperty("24x24")
            String size24x24,
            @JsonProperty("16x16")
            String size16x16,
            @JsonProperty("32x32")
            String size32x32
    ) {}

    /**
     * Represents API links for the response
     */
    public record Links(
            String self,
            String base,
            String context
    ) {}
}

