package com.company.resumeai.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeFragmentRepository extends JpaRepository<KnowledgeFragment, UUID> {

    // Spring Data can't express pgvector's `<=>` (cosine distance) operator
    // declaratively, so this is a native query. Each filter is applied only
    // when its parameter is non-null (§13 step 1: structured filters before
    // vector search). Cast :queryVector to ::vector explicitly since the
    // driver sends it as text.
    @Query(value = """
            SELECT * FROM knowledge_fragment
            WHERE embedding IS NOT NULL
              AND (:domain IS NULL OR domain = :domain)
              AND (:role IS NULL OR role = :role)
              AND (:clientId IS NULL OR client_id = :clientId)
              AND (:startYear IS NULL OR end_year IS NULL OR end_year >= :startYear)
              AND (:endYear IS NULL OR start_year IS NULL OR start_year <= :endYear)
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeFragment> findSimilar(@Param("queryVector") String queryVector,
                                          @Param("domain") String domain,
                                          @Param("role") String role,
                                          @Param("clientId") UUID clientId,
                                          @Param("startYear") Integer startYear,
                                          @Param("endYear") Integer endYear,
                                          @Param("limit") int limit);

    // Derived delete query (Spring Data generates the JPQL) - used when a
    // resume_source is deleted, so its fragments don't become orphaned
    // duplicates sitting in the knowledge base forever (the FK itself is
    // ON DELETE SET NULL, not CASCADE - see V4 migration - so this has to be
    // done explicitly, not left to the database).
    long deleteBySourceResumeId(UUID sourceResumeId);
}
