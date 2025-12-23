package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bean representing a single Jira Service Desk Comment.
 *
 * Corresponds to: GET /rest/servicedeskapi/request/{requestIdOrKey}/comment/{commentId}
 * or individual comment objects from the comments list response
 */
public record AddCommentResult(
        @JsonProperty("_expands")
        List<String> expands,
        String id,
        String body,
        @JsonProperty("public")
        boolean isPublic,
        User author,
        DateInfo created,
        @JsonProperty("_links")
        Links links
) {
    /**
     * Represents a user (comment author)
     */
    public record User(
            String accountId,
            String name,
            String key,
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
            @JsonProperty("16x16")
            String size16x16,
            @JsonProperty("24x24")
            String size24x24,
            @JsonProperty("32x32")
            String size32x32,
            @JsonProperty("48x48")
            String size48x48
    ) {}

    /**
     * Represents date/time information in multiple formats
     */
    public record DateInfo(
            long epochMillis,
            String friendly,
            String iso8601,
            String jira
    ) {}

    /**
     * Represents API links for the comment
     */
    public record Links(
            String self
    ) {}
}

