package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bean representing the Jira Service Desk API response for listing all comments on a request.
 *
 * Corresponds to: GET /rest/servicedeskapi/request/{requestIdOrKey}/comment
 */
public record Comments(
        int size,
        int start,
        int limit,
        @JsonProperty("isLastPage")
        boolean isLastPage,
        @JsonProperty("_links")
        Links links,
        List<Comment> values
) {
    /**
     * Represents a single comment on a service desk request
     */
    public record Comment(
            @JsonProperty("_expands")
            List<String> expands,
            String id,
            String body,
            @JsonProperty("public")
            boolean isPublic,
            User author,
            DateInfo created,
            @JsonProperty("_links")
            CommentLinks commentLinks
    ) {}

    /**
     * Represents a user (comment author)
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
     * Represents date/time information in multiple formats
     */
    public record DateInfo(
            String iso8601,
            String jira,
            String friendly,
            long epochMillis
    ) {}

    /**
     * Represents comment-specific links
     */
    public record CommentLinks(
            String self
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

