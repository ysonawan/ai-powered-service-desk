package com.company.ai.help.desk.integrations;

import com.company.ai.help.desk.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Jira Client for REST API integration
 * Handles all Jira operations:
 * Configuration required in application.yml:
 *   jira:
 *     url: https://your-domain.atlassian.net
 *     bearer-token: your-bearer-token
 */
@Slf4j
@Component
public class JiraClient {

    @Value("${jira.url:https://your-domain.atlassian.net}")
    private String jiraUrl;

    @Value("${jira.basic-token:}")
    private String basicToken;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Creates HTTP headers with Bearer token authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicToken);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Fetches all service desks from Jira Service Management
     */
    public ServiceDesk getServiceDesks() {
        log.info("Getting all service desks from JSM");
        String apiUrl = jiraUrl + "/rest/servicedeskapi/servicedesk";
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<ServiceDesk> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                ServiceDesk.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Successfully fetched {} Service Desks from JSM API", response.getBody().size());
            return response.getBody();
        } else {
            log.warn("JSM API returned non-success status: {}", response.getStatusCode());
            return null;
        }
    }

    /**
     * Fetches all request types available for a service desk
     * @param serviceDeskId The Service Desk ID
     * @return ServiceDeskRequestTypesResponse containing all available request types
     */
    public RequestTypes getServiceDeskRequestTypes(String serviceDeskId) {
        log.info("Getting request types for the service desk: {}", serviceDeskId);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/servicedesk/" + serviceDeskId + "/requesttype";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<RequestTypes> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    RequestTypes.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} request types from JSM API for service desk: {}",
                        response.getBody().size(), serviceDeskId);
                return response.getBody();
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching request types for service desk: {}", serviceDeskId, e);
            return null;
        }
    }

    /**
     * Fetches raw Service Desk Request (ticket) by ticket ID
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @return ServiceDeskRequest bean with full ticket details
     */
    public ServiceDeskTicket getServiceDeskTicketDetails(String ticketId) {
        log.info("Fetching Service Desk Request: {}", ticketId);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/request/" + ticketId;
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<ServiceDeskTicket> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    ServiceDeskTicket.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched Service Desk Request from JSM API: {}", ticketId);
                return response.getBody();
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching Service Desk Request from Jira", e);
            return null;
        }
    }

    /**
     * Fetches Service Desk Request comments by ticket ID
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @return ServiceDeskCommentsResponse containing all comments on the request
     */
    public Comments getServiceDeskTicketComments(String ticketId) {
        log.info("Fetching Service Desk Request Comments: {}", ticketId);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/request/" + ticketId + "/comment";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Comments> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    Comments.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} comments from JSM API for request: {}",
                        response.getBody().size(), ticketId);
                return response.getBody();
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching Service Desk Request comments from Jira", e);
            return null;
        }
    }


    /**
     * Adds a comment to a ticket
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param commentBody The comment text to add
     * @return AddCommentResult with the created comment details
     */
    public AddCommentResult addServiceDeskTicketComment(String ticketId, String commentBody) {
        return addServiceDeskTicketComment(ticketId, commentBody, true);
    }

    /**
     * Adds a comment to a ticket with visibility control
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param commentBody The comment text to add
     * @param isPublic Whether the comment should be visible to customers
     * @return AddCommentResult with the created comment details
     */
    public AddCommentResult addServiceDeskTicketComment(String ticketId, String commentBody, boolean isPublic) {
        log.info("Adding comment to ticket: {} (public: {})", ticketId, isPublic);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/request/" + ticketId + "/comment";

        try {
            // Create the comment request body with proper JSON escaping
            String escapedBody = commentBody
                    .replace("\\", "\\\\")  // Escape backslashes first
                    .replace("\"", "\\\"") // Then escape quotes
                    .replace("\n", "\\n")  // Escape newlines
                    .replace("\r", "\\r"); // Escape carriage returns

            String requestBody = String.format(
                    "{\"body\":\"%s\",\"public\":%b}",
                    escapedBody,
                    isPublic
            );

            log.debug("Request body: {}", requestBody);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<AddCommentResult> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    AddCommentResult.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully added comment to ticket: {}", ticketId);
                return response.getBody();
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error adding comment to ticket", e);
            return null;
        }
    }

    /**
     * Creates a new Service Desk ticket
     * @param serviceDeskId The Service Desk ID
     * @param requestTypeId The Request Type ID
     * @param summary The ticket summary
     * @param description The ticket description
     * @return ServiceDeskTicket with the created ticket details
     */
    public ServiceDeskTicket createServiceDeskTicket(
            String serviceDeskId,
            String requestTypeId,
            String summary,
            String description) {

        log.info("Creating Service Desk ticket in service desk: {}, request type: {}", serviceDeskId, requestTypeId);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/request";

        try {
            // Build the request body using Jackson to handle JSON serialization
            CreateServiceDeskTicketRequest.RequestFieldValues requestFieldValues =
                    new CreateServiceDeskTicketRequest.RequestFieldValues(description, summary);

            CreateServiceDeskTicketRequest createRequest = new CreateServiceDeskTicketRequest(
                    null,  // form data - optional
                    false, // isAdfRequest
                    requestFieldValues,
                    null,  // requestParticipants - optional
                    requestTypeId,
                    serviceDeskId
            );

            HttpHeaders headers = createAuthHeaders();
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.putAll(headers);
            jsonHeaders.set("Content-Type", "application/json");

            HttpEntity<CreateServiceDeskTicketRequest> entity = new HttpEntity<>(createRequest, jsonHeaders);

            ResponseEntity<ServiceDeskTicket> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    ServiceDeskTicket.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully created Service Desk ticket: {}", response.getBody().issueKey());
                return response.getBody();
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating Service Desk ticket", e);
            return null;
        }
    }

    /**
     * Searches for tickets using JQL (Jira Query Language)
     * @param jql The JQL query string (e.g., "project = HSP AND status = Done")
     * @param maxResults Maximum number of results to return (default: 50)
     * @return JiraSearchResult containing matching tickets
     */
    public JiraSearchResult searchTicketsByJql(String jql, int maxResults) {

        log.info("Searching Jira tickets with JQL: {}", jql);

        try {
            URI apiUri = UriComponentsBuilder
                    .fromHttpUrl(jiraUrl)
                    .path("/rest/api/3/search/jql")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", maxResults)
                    .queryParam("fields", "*all")
                    .build()
                    .encode()
                    .toUri();

            log.debug("Jira search URL: {}", apiUri);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraSearchResult> response =
                    restTemplate.exchange(
                            apiUri,
                            HttpMethod.GET,
                            entity,
                            JiraSearchResult.class
                    );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Jira search successful. Total: {}", response.getBody().total());
                return response.getBody();
            }

            log.warn("Jira returned non-success status: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            log.error("Jira search failed. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            log.error("Unexpected error during Jira search", e);
            return null;
        }
    }

    /**
     * Updates the rating (CSAT feedback) for a ticket
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param rating The CSAT rating score (typically 1-5)
     * @param feedback Optional comment/feedback text
     * @return true if rating was submitted successfully, false otherwise
     */
    public boolean updateRating(String ticketId, int rating, String feedback) {
        log.info("Updating rating for ticket: {} with score: {}", ticketId, rating);
        String apiUrl = jiraUrl + "/rest/servicedeskapi/request/" + ticketId + "/feedback";

        try {
            // Build the comment section if feedback is provided
            String commentSection = "";
            if (feedback != null && !feedback.isEmpty()) {
                String escapedFeedback = feedback
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                commentSection = String.format(",\"comment\":{\"body\":\"%s\"}", escapedFeedback);
            }

            String requestBody = String.format(
                    "{\"rating\":%d,\"type\":\"csat\"%s}",
                    rating,
                    commentSection
            );

            log.debug("Rating request body: {}", requestBody);

            HttpHeaders headers = createAuthHeaders();
            headers.set("X-ExperimentalApi", "opt-in");
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully submitted rating for ticket: {}", ticketId);
                return true;
            } else {
                log.warn("JSM API returned non-success status: {}", response.getStatusCode());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.error("Error updating rating. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Error updating rating", e);
            return false;
        }
    }

    /**
     * Fetches available transitions for a Service Desk ticket
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @return TicketTransitions containing all available transitions for the ticket
     */
    public TicketTransitions getServiceDeskTicketTransitions(String ticketId) {
        log.info("Fetching Service Desk Request Transitions: {}", ticketId);
        String apiUrl = jiraUrl + "/rest/api/3/issue/" + ticketId + "/transitions";

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<TicketTransitions> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    TicketTransitions.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} transitions from Jira API for ticket: {}",
                        response.getBody().transitions() != null ? response.getBody().transitions().size() : 0,
                        ticketId);
                return response.getBody();
            } else {
                log.warn("Jira API returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching Service Desk Request Transitions from Jira", e);
            return null;
        }
    }

    /**
     * Transitions a ticket to a different status
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param transitionId The ID of the transition to perform (e.g., "101")
     * @return true if transition was successful, false otherwise
     */
    public boolean updateServiceDeskTicketTransitions(String ticketId, String transitionId) {
        return updateServiceDeskTicketTransitions(ticketId, transitionId, null, null);
    }

    /**
     * Transitions a ticket to a different status with optional fields
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param transitionId The ID of the transition to perform (e.g., "101")
     * @param fields Optional field values to update during transition
     * @return true if transition was successful, false otherwise
     */
    public boolean updateServiceDeskTicketTransitions(String ticketId, String transitionId, java.util.Map<String, Object> fields) {
        return updateServiceDeskTicketTransitions(ticketId, transitionId, fields, null);
    }

    /**
     * Transitions a ticket to a different status with optional fields and history metadata
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @param transitionId The ID of the transition to perform (e.g., "101")
     * @param fields Optional field values to update during transition
     * @param historyMetadata Optional metadata for tracking the change
     * @return true if transition was successful, false otherwise
     */
    public boolean updateServiceDeskTicketTransitions(String ticketId, String transitionId, java.util.Map<String, Object> fields, TransitionTicketRequest.HistoryMetadata historyMetadata) {
        log.info("Transitioning Service Desk ticket: {} to transition: {}", ticketId, transitionId);
        String apiUrl = jiraUrl + "/rest/api/3/issue/" + ticketId + "/transitions";

        try {
            // Build the transition request
            TransitionTicketRequest transitionRequest = new TransitionTicketRequest.Builder()
                    .withTransitionId(transitionId)
                    .withFields(fields)
                    .withHistoryMetadata(historyMetadata)
                    .build();

            HttpHeaders headers = createAuthHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<TransitionTicketRequest> entity = new HttpEntity<>(transitionRequest, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully transitioned ticket: {} to transition: {}", ticketId, transitionId);
                return true;
            } else {
                log.warn("Jira API returned non-success status: {}", response.getStatusCode());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.error("Error transitioning ticket. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (RestClientException e) {
            log.error("Error transitioning Service Desk ticket: {}", ticketId, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error transitioning Service Desk ticket", e);
            return false;
        }
    }

    /**
     * Resolves a service desk ticket by transitioning it to a resolved/done state
     * Implements a 3-attempt retry mechanism:
     * - Attempt 1: Look for direct resolve transition (Done, Resolved, etc.)
     * - Attempt 2: Transition to In Review/Review status, then look for resolve transition
     * - Attempt 3: Transition to In Progress status, then look for resolve transition
     * @param ticketId Issue ID or Issue Key (e.g., "10007" or "BITSUP-1")
     * @return The resolved ServiceDeskTicket, or null if resolution failed after all attempts
     */
    public ServiceDeskTicket resolveServiceDeskTicket(String ticketId) {
        log.info("Resolving service desk ticket: {} (up to 3 attempts)", ticketId);

        try {
            int maxAttempts = 3;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.info("Attempt {} of {} to resolve ticket: {}", attempt, maxAttempts, ticketId);

                // Get available transitions
                TicketTransitions ticketTransitions = getServiceDeskTicketTransitions(ticketId);
                if (ticketTransitions == null || ticketTransitions.transitions() == null || ticketTransitions.transitions().isEmpty()) {
                    log.warn("No transitions available for ticket: {} on attempt {}", ticketId, attempt);
                    if (attempt < maxAttempts) {
                        continue;
                    }
                    return null;
                }

                // First attempt: Look for direct resolve transition
                if (attempt == 1) {
                    String resolveTransitionId = findResolveTransition(ticketTransitions);
                    if (resolveTransitionId != null) {
                        log.info("Found direct resolve transition on attempt 1: {}", resolveTransitionId);
                        if (applyTransitionAndVerify(ticketId, resolveTransitionId)) {
                            return getServiceDeskTicketDetails(ticketId);
                        }
                    } else {
                        log.warn("No resolve transition found on attempt 1, will try intermediate transitions");
                    }
                }

                // Attempt 2: Try to transition to Review status
                if (attempt == 2) {
                    String reviewTransitionId = findTransitionToStatus(ticketTransitions, new String[]{"Review", "In Review", "Under Review"}, new String[]{"review"});
                    if (reviewTransitionId != null) {
                        log.info("Found review transition on attempt 2: {}", reviewTransitionId);
                        if (applyTransitionAndVerify(ticketId, reviewTransitionId)) {
                            log.info("Successfully transitioned to Review status on attempt 2");
                            continue; // Continue to next attempt to look for resolve transition
                        }
                    } else {
                        log.warn("No review transition found on attempt 2, will try in-progress transition");
                    }
                }

                // Attempt 3: Try to transition to In Progress status
                if (attempt == 3) {
                    String inProgressTransitionId = findTransitionToStatus(ticketTransitions, new String[]{"In Progress", "In progress", "Ongoing"}, new String[]{"indeterminate", "in-progress"});
                    if (inProgressTransitionId != null) {
                        log.info("Found in-progress transition on attempt 3: {}", inProgressTransitionId);
                        if (applyTransitionAndVerify(ticketId, inProgressTransitionId)) {
                            log.info("Successfully transitioned to In Progress status on attempt 3");
                            // After transitioning to in-progress, try to find resolve transition one more time
                            TicketTransitions finalTransitions = getServiceDeskTicketTransitions(ticketId);
                            if (finalTransitions != null && finalTransitions.transitions() != null) {
                                String resolveTransitionId = findResolveTransition(finalTransitions);
                                if (resolveTransitionId != null) {
                                    log.info("Found resolve transition after in-progress transition: {}", resolveTransitionId);
                                    if (applyTransitionAndVerify(ticketId, resolveTransitionId)) {
                                        return getServiceDeskTicketDetails(ticketId);
                                    }
                                }
                            }
                        }
                    } else {
                        log.warn("No in-progress transition found on attempt 3");
                    }
                }
            }

            log.warn("Failed to resolve ticket: {} after {} attempts", ticketId, maxAttempts);
            return null;

        } catch (Exception e) {
            log.error("Error resolving service desk ticket: {}", ticketId, e);
            return null;
        }
    }

    /**
     * Applies a transition and verifies success
     * @param ticketId Issue ID or Issue Key
     * @param transitionId The transition ID to apply
     * @return true if transition was applied successfully
     */
    private boolean applyTransitionAndVerify(String ticketId, String transitionId) {
        log.info("Applying transition {} to ticket: {}", transitionId, ticketId);
        boolean success = updateServiceDeskTicketTransitions(ticketId, transitionId);
        if (success) {
            log.info("Successfully applied transition {} to ticket: {}", transitionId, ticketId);
        } else {
            log.warn("Failed to apply transition {} to ticket: {}", transitionId, ticketId);
        }
        return success;
    }

    /**
     * Finds a transition ID that leads to a "Done" or "Resolved" status
     * @param ticketTransitions Available transitions for the ticket
     * @return The ID of a resolve transition, or null if none found
     */
    private String findResolveTransition(TicketTransitions ticketTransitions) {
        if (ticketTransitions == null || ticketTransitions.transitions() == null) {
            return null;
        }

        // Look for transitions to "Done", "Resolved", "Closed", or similar statuses
        String[] resolveStatusNames = {"Done", "Resolved", "Closed", "Complete", "Completed"};
        String[] resolveStatusKeys = {"done", "resolved", "closed", "complete", "completed"};

        for (TicketTransitions.Transition transition : ticketTransitions.transitions()) {
            if (transition == null || transition.to() == null) {
                continue;
            }

            String statusName = transition.to().name();
            String statusKey = transition.to().statusCategory() != null ?
                    transition.to().statusCategory().key() : "";

            // Check if this transition leads to a resolve status
            for (String resolveName : resolveStatusNames) {
                if (statusName != null && statusName.equalsIgnoreCase(resolveName)) {
                    log.debug("Found resolve transition {} to status: {}", transition.id(), statusName);
                    return transition.id();
                }
            }

            for (String resolveKey : resolveStatusKeys) {
                if (statusKey != null && statusKey.equalsIgnoreCase(resolveKey)) {
                    log.debug("Found resolve transition {} to status: {}", transition.id(), statusKey);
                    return transition.id();
                }
            }
        }

        log.warn("No resolve transition found among {} available transitions", ticketTransitions.transitions().size());
        return null;
    }

    /**
     * Finds a transition ID that leads to a specific status by name or key
     * @param ticketTransitions Available transitions for the ticket
     * @param statusNames Array of status names to look for (case-insensitive)
     * @param statusKeys Array of status category keys to look for (case-insensitive)
     * @return The ID of the matching transition, or null if none found
     */
    private String findTransitionToStatus(TicketTransitions ticketTransitions, String[] statusNames, String[] statusKeys) {
        if (ticketTransitions == null || ticketTransitions.transitions() == null) {
            return null;
        }

        for (TicketTransitions.Transition transition : ticketTransitions.transitions()) {
            if (transition == null || transition.to() == null) {
                continue;
            }

            String transitionStatusName = transition.to().name();
            String transitionStatusKey = transition.to().statusCategory() != null ?
                    transition.to().statusCategory().key() : "";

            // Check if this transition leads to one of the target statuses
            for (String statusName : statusNames) {
                if (transitionStatusName != null && transitionStatusName.equalsIgnoreCase(statusName)) {
                    log.debug("Found transition {} to status: {}", transition.id(), statusName);
                    return transition.id();
                }
            }

            for (String statusKey : statusKeys) {
                if (transitionStatusKey != null && transitionStatusKey.equalsIgnoreCase(statusKey)) {
                    log.debug("Found transition {} to status key: {}", transition.id(), statusKey);
                    return transition.id();
                }
            }
        }

        log.debug("No transition found to statuses: {} or status keys: {}", java.util.Arrays.toString(statusNames), java.util.Arrays.toString(statusKeys));
        return null;
    }

}

