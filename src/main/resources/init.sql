
-- Create database
CREATE DATABASE helpdesk_rag;

-- Connect to the new database
\c helpdesk_rag

CREATE USER helpdesk_user WITH PASSWORD 'helpdesk_password'

-- Install pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create knowledge_chunks table
CREATE TABLE IF NOT EXISTS knowledge_chunks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id TEXT NOT NULL,
  source_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  source_title TEXT,
  content TEXT NOT NULL,
  embedding vector(768) NOT NULL,
  metadata JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
ON knowledge_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Create index for tenant lookups
CREATE INDEX IF NOT EXISTS idx_chunks_tenant_id
ON knowledge_chunks(tenant_id);

-- Create index for source lookups
CREATE INDEX IF NOT EXISTS idx_chunks_source_id
ON knowledge_chunks(source_id);

-- Create index for created_at lookups
CREATE INDEX IF NOT EXISTS idx_chunks_created_at
ON knowledge_chunks(created_at);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_chunks_tenant_source
ON knowledge_chunks(tenant_id, source_type);

-- Grant permissions on database
GRANT ALL PRIVILEGES ON DATABASE helpdesk_rag TO helpdesk_user;

-- Grant permissions on schema (public)
GRANT ALL PRIVILEGES ON SCHEMA public TO helpdesk_user;

-- Grant permissions on table
GRANT ALL PRIVILEGES ON TABLE knowledge_chunks TO helpdesk_user;

-- Grant permissions on sequences (for auto-increment if any)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO helpdesk_user;

-- Grant permissions on functions (for any stored procedures)
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO helpdesk_user;

