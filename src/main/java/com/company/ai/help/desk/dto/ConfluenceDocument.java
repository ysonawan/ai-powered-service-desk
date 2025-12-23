package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Confluence document API response
 * Represents a single Confluence page/document
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfluenceDocument(
        String id,
        String title,
        String status,
        String spaceId,
        String parentId,
        String parentType,
        String ownerId,
        String authorId,
        String createdAt,
        Version version,
        Body body,
        Links links
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Version(
            int number,
            String message,
            boolean minorEdit,
            String authorId,
            String createdAt,
            String ncsStepVersion
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Storage storage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Storage(
            String representation,
            String value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Links(
            @JsonProperty("editui")
            String editui,
            @JsonProperty("webui")
            String webui,
            @JsonProperty("edituiv2")
            String edituiv2,
            @JsonProperty("tinyui")
            String tinyui,
            @JsonProperty("base")
            String base
    ) {}
}

