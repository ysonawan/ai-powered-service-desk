# RAG Implementation in MCP Server

## Overview
This MCP server implements Retrieval-Augmented Generation (RAG) to enhance support ticket resolution by searching historical tickets and knowledge base (KB) articles using vector embeddings.

## How RAG is Implemented
- **Text Cleaning**: Raw ticket content is cleaned (HTML, URLs, markdown, special chars removed) and normalized using `TextCleaningService`.
- **Chunking**: Cleaned text is split into overlapping chunks (default: 1000 chars, 200 overlap) for better embedding and retrieval.
- **Embedding**: Each chunk is embedded using a local LLM embedding API (E5-base, 768-dim vectors) via `EmbeddingApiClient`. The API is called at `http://localhost:8001/embed`.
- **Storage**: Embeddings and chunk metadata are stored in PostgreSQL using the `pgvector` extension. Each chunk is stored as a `KnowledgeChunk` entity.
- **Indexing**: Chunks are indexed by tenant, source type, and other metadata for efficient retrieval.

## Data Ingestion into Vector DB
- **One-Time Ingestion**: When application starts and no knowledge available, service fetches all resolved tickets and knowledge documents from confluence, it is cleaned, chunked, embedded, and stored in the vector DB. This can be triggered manually or as part of a workflow.
- **Scheduled Ingestion**: The scheduled job (e.g., every 30 minutes) to automatically ingest new closed tickets from Jira into the knowledge base. This ensures the vector DB stays up-to-date with the latest resolved tickets.

## Retrieval (RAG Query)
- **Query Cleaning & Embedding**: User queries are cleaned and embedded using the same pipeline.
- **Vector Search**: The system performs a vector similarity search (using pgvector) to find the most relevant knowledge chunks for the query.
- **Selection Criteria**:
  - **Limit**: Maximum number of results returned (e.g., top 5 or 10).
  - **Score Threshold**: Only results above a certain similarity score are returned (configurable per query/tool call).
- **Tool Usage**: The main tool for RAG is `retrieveServiceDeskKnowledge`, which takes `tenantId`, `query`, `limit`, and `thresholdScore` as parameters and returns relevant KB/ticket chunks.

## Local LLM Embedding API Setup
This project uses a local E5-base embedding model served via FastAPI. No Docker is required.

**Step 1 — Create a Python virtual environment**
```bash
python3 -m venv e5-env
source e5-env/bin/activate
```

**Step 2 — Install required libraries**
```bash
pip install fastapi uvicorn sentence-transformers torch
```

**Step 3 — Create the embedding API**
Create a file: `embedding_server.py`
```python
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# Load model once at startup
model = SentenceTransformer("intfloat/e5-base")

app = FastAPI(title="Local E5 Embedding Service")

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    embedding: list[float]
    model: str
    dimensions: int

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    # E5 expects "query:" or "passage:" prefix
    text = "passage: " + req.text
    vector = model.encode(text, normalize_embeddings=True)

    return {
        "embedding": vector.tolist(),
        "model": "intfloat/e5-base",
        "dimensions": len(vector)
    }
```

**Step 4 — Start the embedding server**
```bash
uvicorn embedding_server:app --host 0.0.0.0 --port 8001
```

**Step 5 — Test the API**
```bash
curl -X POST http://localhost:8001/embed \
  -H "Content-Type: application/json" \
  -d '{"text":"Jenkins pipeline failing with permission denied"}'
```

You should receive:
```json
{
  "embedding": [0.0123, -0.334, ...],
  "model": "intfloat/e5-base",
  "dimensions": 768
}
```

✅ Your local embedding model is working.

## Example Flow
1. A user reports an issue.
2. The system cleans and embeds the query.
3. It searches the KB for similar chunks using vector similarity.
4. If relevant results (above threshold) are found, they are returned as possible solutions.
5. If not, a new support ticket is created.

## Summary
- Embeddings are generated locally (no external LLM API required).
- All retrieval is based on vector similarity (RAG) with configurable limit and score.
- The RAG pipeline is modular: cleaning → chunking → embedding → storage → retrieval.
- Data is ingested both on-demand (one-time) and via scheduled jobs to keep the knowledge base current.

---
This document summarizes the RAG implementation, embedding pipeline, retrieval criteria, ingestion strategy, and local LLM setup in the MCP server. For more details, see `KnowledgeBaseService`, `EmbeddingApiClient`, and `HelpDeskTools` classes.
