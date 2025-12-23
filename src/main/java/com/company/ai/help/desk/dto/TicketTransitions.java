package com.company.ai.help.desk.dto;

import java.util.List;

/**
 * Bean representing the Jira API response for ticket transitions.
 *
 * Corresponds to: GET /rest/api/3/issue/{issueIdOrKey}/transitions
 */
public record TicketTransitions(
        String expand,
        List<Transition> transitions
) {
    /**
     * Represents a single transition option
     */
    public record Transition(
            String id,
            String name,
            TransitionStatus to,
            boolean hasScreen,
            boolean isGlobal,
            boolean isInitial,
            boolean isAvailable,
            boolean isConditional,
            boolean isLooped
    ) {}

    /**
     * Represents the target status of a transition
     */
    public record TransitionStatus(
            String self,
            String description,
            String iconUrl,
            String name,
            String id,
            StatusCategory statusCategory
    ) {}

    /**
     * Represents the status category
     */
    public record StatusCategory(
            String self,
            int id,
            String key,
            String colorName,
            String name
    ) {}
}

