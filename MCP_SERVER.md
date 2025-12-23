# MCP Server Overview

## Tech Stack
- **Java 21**
- **Spring Boot 3.5.8**
- **Spring AI (MCP Server WebMVC)**
- **PostgreSQL** (with [pgvector](https://github.com/pgvector/pgvector) for vector storage)

## Main Components

### Tools
- **HelpDeskTools**:
  - `retrieveServiceDeskKnowledge`
  - `getServiceDesks`
  - `getServiceDeskRequestTypes`
  - `getServiceDeskTicketDetails`
  - `addServiceDeskTicketComment`
  - `getServiceDeskTicketComments`
  - `createServiceDeskTicket`
  - `resolveServiceDeskTicket`
  - `rateServiceDeskTicketSolution`
  
### Resources
- **HelpDeskResourceProvider**:
  - `servicedesk://projects`
  - `policy://handle-request`

### Prompts
- **HelpDeskPromptProvider**:
  - `service-desk-agent`
  - `list-service-desks`
  - `report-issue`
  - `search-similar-tickets`
  - `ticket-details`
  - `create-support-ticket`
  - `resolve-ticket`
  - `add-ticket-comment`
  - `rate-solution`

### Services
- **KnowledgeBaseService**: Handles cleaning, chunking, embedding, and storage of knowledge documents. Supports vector search with limit and score threshold.
- **ClosedTicketSyncService**: Handles scheduled ingestion of closed tickets into the knowledge base (see below).

### Integrations
- **JiraTicketEmbeddingService**: Integrates with Jira to process closed tickets and store them in the knowledge base with embeddings.
- **ConfluenceEmbeddingService**: Integrates with Confluence to process knowledge documents and store them in the knowledge base with embeddings.
- **JiraClient**: Handles Jira API interactions for tools
- **ConfluenceClient**: Handles Confluence API interactions for tools
- **EmbeddingApiClient**: Calls a local embedding API (E5-base model) to generate vector embeddings for text.

## How It Works
- User queries are processed by the MCP server using tools and prompts.
- Knowledge and historical tickets are embedded and stored as vectors in PostgreSQL (via pgvector).
- Tools and resources are exposed for the AI model to use in reasoning and ticket management.

---
This document gives a concise overview of the MCP server's architecture, tech stack, and main components (tools, resources, prompts, services, integrations, and storage). For more details, see the code and specific class documentation.
