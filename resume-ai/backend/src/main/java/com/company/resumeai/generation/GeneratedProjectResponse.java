package com.company.resumeai.generation;

import com.company.resumeai.similarity.SimilarityVerdict;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GeneratedProjectResponse(
        UUID id,
        String clientName,
        String roleTitle,
        LocalDate startDate,
        LocalDate endDate,
        String domain,
        String description,
        List<String> responsibilities,
        List<String> environment,
        String promptVersion,
        Double similarityScore,
        SimilarityVerdict similarityVerdict,
        boolean duplicatePhraseDetected,
        int rewriteAttempts
) {

    public static GeneratedProjectResponse from(GeneratedProject project) {
        return new GeneratedProjectResponse(
                project.getId(),
                project.getClientName(),
                project.getRoleTitle(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDomain(),
                project.getDescription(),
                project.getResponsibilities(),
                project.getEnvironment(),
                project.getPromptVersion(),
                project.getSimilarityScore(),
                project.getSimilarityVerdict(),
                project.isDuplicatePhraseDetected(),
                project.getRewriteAttempts()
        );
    }
}
