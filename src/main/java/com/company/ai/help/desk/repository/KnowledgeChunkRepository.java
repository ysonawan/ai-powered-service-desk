package com.company.ai.help.desk.repository;

import com.company.ai.help.desk.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for KnowledgeChunk entity with custom queries for RAG.
 */
@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    @Query(value = "SELECT * FROM knowledge_chunks as kc " +
            "WHERE kc.tenant_id = lower(:tenantId) " +
            "ORDER BY kc.embedding <=> cast(:embedding as vector) LIMIT :limit",
            nativeQuery = true)
    List<KnowledgeChunk> findSimilarChunks(
            @Param("tenantId") String tenantId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);

    @Query(value = "SELECT (kc.embedding <=> cast(:embedding as vector)) as distance " +
            "FROM knowledge_chunks as kc " +
            "WHERE kc.id = :id ",
            nativeQuery = true)
    double getEmbeddingDistance(
            @Param("embedding") String embedding,
            @Param("id") UUID id);

    long deleteBySourceId(String sourceId);

    boolean existsBySourceIdAndSourceType(String sourceId, String sourceType);

    boolean existsBySourceType(String sourceType);
}

