package com.company.ai.help.desk.dto;

import java.util.Map;

/**
 * Bean representing the Jira API request payload for transitioning an issue.
 * Corresponds to: POST /rest/api/3/issue/{issueIdOrKey}/transitions
 * <p>
 * Example payload:
 * {
 *   "transition": {
 *     "id": "101"
 *   },
 *   "fields": {
 *     "resolution": {
 *       "name": "Fixed"
 *     }
 *   },
 *   "update": {},
 *   "historyMetadata": {
 *     "type": "mcp-automation",
 *     "description": "Transitioned via MCP Service"
 *   }
 * }
 */
public record TransitionTicketRequest(
        Transition transition,
        Map<String, Object> fields,
        Map<String, Object> update,
        HistoryMetadata historyMetadata
) {
    /**
     * Represents the transition to perform
     */
    public record Transition(
            String id
    ) {}

    /**
     * Represents history metadata for tracking the change
     */
    public record HistoryMetadata(
            String type,
            String description,
            String activityDescription,
            String actor,
            String generator,
            String cause,
            long created
    ) {}

    /**
     * Builder to simplify construction
     */
    public static class Builder {
        private String transitionId;
        private Map<String, Object> fields;
        private Map<String, Object> update;
        private HistoryMetadata historyMetadata;

        public Builder withTransitionId(String transitionId) {
            this.transitionId = transitionId;
            return this;
        }

        public Builder withFields(Map<String, Object> fields) {
            this.fields = fields;
            return this;
        }

        public Builder withUpdate(Map<String, Object> update) {
            this.update = update;
            return this;
        }

        public Builder withHistoryMetadata(HistoryMetadata historyMetadata) {
            this.historyMetadata = historyMetadata;
            return this;
        }

        public TransitionTicketRequest build() {
            return new TransitionTicketRequest(
                    new Transition(transitionId),
                    fields != null ? fields : Map.of(),
                    update != null ? update : Map.of(),
                    historyMetadata
            );
        }
    }
}

