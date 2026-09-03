package com.company.resumeai.knowledge;

import com.company.resumeai.embedding.VectorCodec;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

/**
 * candidate_id / client_id / project_id are plain UUID FKs (nullable, as
 * intended per §8.7 - a fragment need not belong to all three).
 *
 * `embedding` (Milestone 3): mapped as a plain String field holding
 * pgvector's text literal format (e.g. "[0.12,0.34,...]"), with
 * {@code @ColumnTransformer} casting it to/from the real `vector(1536)`
 * column type at the SQL boundary. Deliberately not using a third-party
 * pgvector-Hibernate integration (pgvector-java, hypersistence-utils) - this
 * is simpler and has no unverified dependency. See VectorCodec for the
 * float[] <-> text conversion, used by KnowledgeFragmentService when writing
 * an embedding and RetrievalService when building a search query vector.
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

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @ColumnTransformer(write = "?::vector", read = "embedding::text")
    private String embedding;

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

    public void applyEmbedding(float[] vector) {
        this.embedding = VectorCodec.encode(vector);
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

    public boolean hasEmbedding() {
        return embedding != null;
    }

    public UUID getSourceResumeId() {
        return sourceResumeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
