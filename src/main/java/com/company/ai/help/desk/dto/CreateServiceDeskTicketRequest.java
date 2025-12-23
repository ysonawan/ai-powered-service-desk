package com.company.ai.help.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Bean representing the request body for creating a Jira Service Desk ticket.
 *
 * Corresponds to: POST /rest/servicedeskapi/request
 */
public record CreateServiceDeskTicketRequest(
        @JsonProperty("form")
        FormData form,
        @JsonProperty("isAdfRequest")
        boolean isAdfRequest,
        @JsonProperty("requestFieldValues")
        RequestFieldValues requestFieldValues,
        @JsonProperty("requestParticipants")
        List<String> requestParticipants,
        String requestTypeId,
        String serviceDeskId
) {
    /**
     * Represents form data for the ticket
     */
    public record FormData(
            Map<String, FormAnswer> answers
    ) {}

    /**
     * Represents a single form answer
     */
    public record FormAnswer(
            String text,
            String date,
            String time,
            List<String> choices,
            List<String> users
    ) {}

    /**
     * Represents the request field values (summary, description, etc.)
     */
    public record RequestFieldValues(
            String description,
            String summary
    ) {}
}

