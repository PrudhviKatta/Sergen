package com.company.resumeai.knowledge;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity + repository only for Milestone 1, per §55/§56.
 *
 * The `embedding vector(1536)` column already exists in the V1 migration (see
 * V1__init_schema.sql) so the table shape matches §8.7, but it is deliberately
 * NOT mapped here yet: no embedding provider is chosen until Milestone 3, and
 * mapping a pgvector column needs either a custom Hibernate UserType or a
 * vector-aware dialect helper, which isn't worth adding before it's used.
 * candidate_id / client_id / project_id are plain UUID FKs (nullable, as
 * intended per §8.7 - a fragment need not belong to all three).
 */
@Entity
@Table(name = "knowledge_fragment")
public class KnowledgeFragment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "project_id")
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fragment_type", nullable = false)
    private FragmentType fragmentType;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "domain")
    private String domain;

    @Column(name = "role")
    private String role;

    @Column(name = "start_year")
    private Short startYear;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "source_resume_id")
    private UUID sourceResumeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected KnowledgeFragment() {
        // JPA
    }

    public KnowledgeFragment(UUID candidateId, UUID clientId, UUID projectId, FragmentType fragmentType,
                              String content, String domain, String role, Short startYear, Short endYear) {
        this.candidateId = candidateId;
        this.clientId = clientId;
        this.projectId = projectId;
        this.fragmentType = fragmentType;
        this.content = content;
        this.domain = domain;
        this.role = role;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public FragmentType getFragmentType() {
        return fragmentType;
    }

    public String getContent() {
        return content;
    }

    public String getDomain() {
        return domain;
    }

    public String getRole() {
        return role;
    }

    public Short getStartYear() {
        return startYear;
    }

    public Short getEndYear() {
        return endYear;
    }

    public UUID getSourceResumeId() {
        return sourceResumeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
