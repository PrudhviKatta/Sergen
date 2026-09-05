package com.company.resumeai.generation;

import com.company.resumeai.similarity.SimilarityCheckResult;
import com.company.resumeai.similarity.SimilarityVerdict;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One project section of a ResumeGeneration (§20 "Client A / Role / Timeline
 * / Project Description / Responsibilities / Environment"). `responsibilities`
 * and `environment` are stored as plain delimited text rather than a separate
 * child table - they're never queried individually, only ever rendered back
 * as a list for the parent generation, so a join table would add mapping
 * complexity with no query benefit.
 */
@Entity
@Table(name = "generated_project")
public class GeneratedProject {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_generation_id", nullable = false)
    private ResumeGeneration resumeGeneration;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "role_title")
    private String roleTitle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "domain")
    private String domain;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "responsibilities", columnDefinition = "text")
    private String responsibilities;

    @Column(name = "environment")
    private String environment;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "similarity_verdict")
    private SimilarityVerdict similarityVerdict;

    @Column(name = "duplicate_phrase_detected", nullable = false)
    private boolean duplicatePhraseDetected;

    @Column(name = "rewrite_attempts", nullable = false)
    private int rewriteAttempts;

    protected GeneratedProject() {
        // JPA
    }

    public GeneratedProject(String clientName, String roleTitle, LocalDate startDate, LocalDate endDate, String domain) {
        this.clientName = clientName;
        this.roleTitle = roleTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.domain = domain;
    }

    void assignTo(ResumeGeneration resumeGeneration) {
        this.resumeGeneration = resumeGeneration;
    }

    public void applyGenerated(String description, List<String> responsibilities, List<String> environment,
                                String promptVersion, SimilarityCheckResult similarityResult, int rewriteAttempts) {
        this.description = description;
        this.responsibilities = String.join("\n", responsibilities);
        this.environment = String.join(", ", environment);
        this.promptVersion = promptVersion;
        this.similarityScore = similarityResult.semanticSimilarity();
        this.similarityVerdict = similarityResult.verdict();
        this.duplicatePhraseDetected = similarityResult.duplicatePhraseDetected();
        this.rewriteAttempts = rewriteAttempts;
    }

    public UUID getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
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

    public String getDescription() {
        return description;
    }

    public List<String> getResponsibilities() {
        return splitOrEmpty(responsibilities, "\n");
    }

    public List<String> getEnvironment() {
        return splitOrEmpty(environment, ", ");
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public SimilarityVerdict getSimilarityVerdict() {
        return similarityVerdict;
    }

    public boolean isDuplicatePhraseDetected() {
        return duplicatePhraseDetected;
    }

    public int getRewriteAttempts() {
        return rewriteAttempts;
    }

    private static List<String> splitOrEmpty(String value, String delimiter) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : value.split(delimiter)) {
            result.add(part);
        }
        return result;
    }
}
