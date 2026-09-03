package com.company.resumeai.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID candidateId,
        UUID clientId,
        String clientName,
        String projectName,
        String roleTitle,
        LocalDate startDate,
        LocalDate endDate,
        String domain,
        String projectSummary,
        BigDecimal confidenceScore,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(CandidateProject project) {
        return new ProjectResponse(
                project.getId(),
                project.getCandidate().getId(),
                project.getClient().getId(),
                project.getClient().getName(),
                project.getProjectName(),
                project.getRoleTitle(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDomain(),
                project.getProjectSummary(),
                project.getConfidenceScore(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
