package com.company.resumeai.generation;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * §21 "Generate Base Resume" / §32 auditability root. Owns its
 * GeneratedProject rows (cascade + orphanRemoval - a generation's projects
 * have no independent lifecycle). Not yet capturing every §32 field
 * (retrieved fragment IDs, similarity score, rewrite attempts, user edits,
 * approval status) - those belong to Milestone 6/7, which don't exist yet.
 */
@Entity
@Table(name = "resume_generation")
public class ResumeGeneration {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(name = "primary_role", nullable = false)
    private String primaryRole;

    @Column(name = "total_experience_years")
    private Integer totalExperienceYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationStatus status;

    @Column(name = "generated_summary", columnDefinition = "text")
    private String generatedSummary;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "model")
    private String model;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "resumeGeneration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startDate ASC")
    private List<GeneratedProject> projects = new ArrayList<>();

    protected ResumeGeneration() {
        // JPA
    }

    public ResumeGeneration(String candidateName, String primaryRole, Integer totalExperienceYears) {
        this.candidateName = candidateName;
        this.primaryRole = primaryRole;
        this.totalExperienceYears = totalExperienceYears;
        this.status = GenerationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addProject(GeneratedProject project) {
        project.assignTo(this);
        this.projects.add(project);
    }

    public void markCompleted(String generatedSummary, String promptVersion, String model) {
        this.generatedSummary = generatedSummary;
        this.promptVersion = promptVersion;
        this.model = model;
        this.status = GenerationStatus.COMPLETED;
    }

    public void markFailed(String reason) {
        this.status = GenerationStatus.FAILED;
        this.failureReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public Integer getTotalExperienceYears() {
        return totalExperienceYears;
    }

    public GenerationStatus getStatus() {
        return status;
    }

    public String getGeneratedSummary() {
        return generatedSummary;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getModel() {
        return model;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GeneratedProject> getProjects() {
        return projects;
    }
}
