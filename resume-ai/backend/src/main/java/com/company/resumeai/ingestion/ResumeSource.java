package com.company.resumeai.ingestion;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * §8.6 resume_source. candidate_id is nullable - most uploads are bulk
 * historical resumes for the knowledge base (§45's "30 curated Java
 * resumes"), not tied to a specific candidate profile being generated for.
 * parsed_json is the LLM's structured output serialized as text (see
 * parser.ParsedResume) - kept as a plain String column rather than a real
 * `jsonb` column + custom Hibernate type, same "don't take on an unverified
 * mapping for one field" reasoning as knowledge.KnowledgeFragment.embedding.
 */
@Entity
@Table(name = "resume_source")
public class ResumeSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "parsed_json", columnDefinition = "text")
    private String parsedJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false)
    private IngestionStatus ingestionStatus;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ResumeSource() {
        // JPA
    }

    public ResumeSource(UUID candidateId, String fileName, String fileType, String rawText) {
        this.candidateId = candidateId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.rawText = rawText;
        this.ingestionStatus = IngestionStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markParsed(String parsedJson) {
        this.parsedJson = parsedJson;
        this.ingestionStatus = IngestionStatus.PARSED;
    }

    public void markFailed(String reason) {
        this.ingestionStatus = IngestionStatus.FAILED;
        this.failureReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getRawText() {
        return rawText;
    }

    public String getParsedJson() {
        return parsedJson;
    }

    public IngestionStatus getIngestionStatus() {
        return ingestionStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
