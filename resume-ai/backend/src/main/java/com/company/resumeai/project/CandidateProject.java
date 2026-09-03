package com.company.resumeai.project;

import com.company.resumeai.candidate.Candidate;
import com.company.resumeai.client.Client;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_project")
public class CandidateProject {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "role_title")
    private String roleTitle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "domain")
    private String domain;

    @Column(name = "project_summary")
    private String projectSummary;

    @Column(name = "source_resume_id")
    private UUID sourceResumeId;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CandidateProject() {
        // JPA
    }

    public CandidateProject(Candidate candidate, Client client, String projectName, String roleTitle,
                             LocalDate startDate, LocalDate endDate, String domain, String projectSummary) {
        this.candidate = candidate;
        this.client = client;
        this.projectName = projectName;
        this.roleTitle = roleTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.domain = domain;
        this.projectSummary = projectSummary;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public Client getClient() {
        return client;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDomain() {
        return domain;
    }

    public String getProjectSummary() {
        return projectSummary;
    }

    public UUID getSourceResumeId() {
        return sourceResumeId;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
