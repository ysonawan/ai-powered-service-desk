package com.company.ai.help.desk.config;

import com.company.ai.help.desk.repository.KnowledgeChunkRepository;
import com.company.ai.help.desk.service.ClosedTicketSyncService;
import com.company.ai.help.desk.service.ConfluenceDocumentSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener that triggers on application startup to initialize the vector table
 * with closed tickets and Confluence documents if the knowledge base is empty.
 */
@Component
@Slf4j
public class ApplicationStartupListener {

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private ClosedTicketSyncService closedTicketSyncService;

    @Autowired
    private ConfluenceDocumentSyncService confluenceDocumentSyncService;

    /**
     * Called when the application is ready to serve requests.
     * Syncs Confluence documents and closed tickets on startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application startup listener triggered");

        try {
            // Sync Confluence documents
            log.info("Syncing Confluence documents");
            confluenceDocumentSyncService.syncConfluenceDocuments();

            // Check if JIRA records exist in knowledge base
            boolean jiraRecordsExist = knowledgeChunkRepository.existsBySourceType("JIRA");

            if (!jiraRecordsExist) {
                log.info("No JIRA records found in knowledge base. Triggering sync of closed tickets");
                String jql = "resolution is not EMPTY ORDER BY updated DESC";
                closedTicketSyncService.syncClosedTickets(jql);
            } else {
                log.info("JIRA records already present in knowledge base. Skipping initial ticket sync");
            }
        } catch (Exception e) {
            log.error("Error during application startup initialization", e);
        }
    }
}



