package com.company.ai.help.desk.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity to store knowledge chunks with embeddings for RAG (Retrieval Augmented Generation).
 */
@Entity
@Table(name = "knowledge_chunks", indexes = {
        @Index(name = "idx_chunks_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_chunks_source_id", columnList = "source_id"),
        @Index(name = "idx_chunks_created_at", columnList = "created_at"),
        @Index(name = "idx_chunks_tenant_source", columnList = "tenant_id,source_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant identifier (e.g., "jenkins", "bitbucket", "infra")
     */
    @Column(nullable = false, length = 100)
    private String tenantId;

    /**
     * Source type: JIRA or CONFLUENCE
     */
    @Column(nullable = false, length = 50)
    private String sourceType;

    /**
     * Source identifier (e.g., JIRA-123 or Confluence pageId)
     */
    @Column(nullable = false, length = 255)
    private String sourceId;

    /**
     * Source title (ticket summary or page title)
     */
    @Column(length = 1000)
    private String sourceTitle;

    /**
     * The actual chunk content (cleaned and normalized text)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Vector embedding (768 dimensions for E5-base model)
     * Uses PGvector type for proper pgvector support.
     */
    @Column(nullable = false, columnDefinition = "vector(768)")
    @Type(PGvectorType.class)
    private PGvector embedding;

    /**
     * Flexible metadata storage (JSON)
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

