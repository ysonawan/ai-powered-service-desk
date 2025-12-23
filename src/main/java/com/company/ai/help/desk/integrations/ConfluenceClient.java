package com.company.ai.help.desk.integrations;

import com.company.ai.help.desk.dto.ConfluenceDocument;
import com.company.ai.help.desk.dto.RequestTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Confluence Client for REST API integration
 * Handles fetching Confluence documents/pages
 * Configuration required in application.yml:
 *   confluence:
 *     url: https://help-desk-mcp-demo.atlassian.net/wiki
 *     bearer-token: your-bearer-token
 */
@Slf4j
@Component
public class ConfluenceClient {

    private final String confluenceUrl;
    private final String basicToken;
    private final RestTemplate restTemplate;

    public ConfluenceClient(@Value("${confluence.url:https://help-desk-mcp-demo.atlassian.net/wiki}") String confluenceUrl,
                           @Value("${confluence.bearer-token:}") String basicToken,
                           RestTemplate restTemplate) {
        this.confluenceUrl = confluenceUrl;
        this.basicToken = basicToken;
        this.restTemplate = restTemplate;
    }

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
     * Fetches a Confluence document by page ID
     *
     * @param pageId The Confluence page ID
     * @return ConfluenceDocument or null if not found
     */
    public ConfluenceDocument getConfluenceDocument(String pageId) {
        try {
            String url = confluenceUrl + "/api/v2/pages/" + pageId + "?body-format=storage";
            log.debug("Fetching Confluence document from: {}", url);
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<ConfluenceDocument> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ConfluenceDocument.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ConfluenceDocument document = response.getBody();
                log.info("Successfully fetched Confluence document with ID: {}", pageId);
                return document;
            }

            log.warn("Unexpected status code {} when fetching Confluence document {}",
                    response.getStatusCode(), pageId);
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Confluence document with ID {} not found", pageId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error {} fetching Confluence document {}: {}",
                    e.getStatusCode(), pageId, e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.error("Error fetching Confluence document {}", pageId, e);
            return null;
        }
    }

    /**
     * Extract plain text from Confluence storage format
     * Removes HTML tags from the storage representation
     *
     * @param storageValue The storage format value containing HTML
     * @return Plain text content
     */
    public String extractPlainText(String storageValue) {
        if (storageValue == null || storageValue.isEmpty()) {
            return "";
        }

        // Remove HTML tags
        String plainText = storageValue.replaceAll("<[^>]*>", "");

        // Decode HTML entities
        plainText = plainText.replace("&amp;", "&");
        plainText = plainText.replace("&lt;", "<");
        plainText = plainText.replace("&gt;", ">");
        plainText = plainText.replace("&quot;", "\"");
        plainText = plainText.replace("&apos;", "'");

        // Remove extra whitespace
        plainText = plainText.replaceAll("\\s+", " ").trim();

        return plainText;
    }
}

