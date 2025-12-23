package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bean representing a Jira Service Desk Request (ticket).
 *
 * Corresponds to: GET /rest/servicedeskapi/request/{requestIdOrKey}
 */
public record ServiceDeskTicket(
        @JsonProperty("_expands")
        List<String> expands,
        String issueId,
        String issueKey,
        String summary,
        String requestTypeId,
        String serviceDeskId,
        @JsonProperty("createdDate")
        DateInfo createdDate,
        User reporter,
        @JsonProperty("requestFieldValues")
        List<RequestField> requestFieldValues,
        @JsonProperty("currentStatus")
        StatusInfo currentStatus,
        @JsonProperty("_links")
        Links links
) {
    /**
     * Represents date/time information in multiple formats
     */
    public record DateInfo(
            String iso8601,
            String jira,
            String friendly,
            long epochMillis
    ) {}

    /**
     * Represents a user (reporter, assignee, etc.)
     */
    public record User(
            String accountId,
            String emailAddress,
            String displayName,
            boolean active,
            String timeZone,
            @JsonProperty("_links")
            UserLinks userLinks
    ) {}

    /**
     * Represents user-related links
     */
    public record UserLinks(
            String jiraRest,
            @JsonProperty("avatarUrls")
            AvatarUrls avatarUrls,
            String self
    ) {}

    /**
     * Represents avatar URLs in different sizes
     */
    public record AvatarUrls(
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
     * Represents a request field value
     * The 'value' field can be either a String or an Object (for complex types like arrays)
     */
    public record RequestField(
            String fieldId,
            String label,
            Object value,
            @JsonProperty("renderedValue")
            RenderedValue renderedValue
    ) {}

    /**
     * Represents rendered HTML value for a field
     */
    public record RenderedValue(
            String html
    ) {}

    /**
     * Represents the current status of the request
     */
    public record StatusInfo(
            String status,
            String statusCategory,
            @JsonProperty("statusDate")
            DateInfo statusDate
    ) {}

    /**
     * Represents API links for the request
     */
    public record Links(
            String jiraRest,
            String web,
            String agent,
            String self
    ) {}
}

