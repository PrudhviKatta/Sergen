package com.company.resumeai.generation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeGenerationResponse(
        UUID id,
        String candidateName,
        String primaryRole,
        Integer totalExperienceYears,
        GenerationStatus status,
        String generatedSummary,
        String promptVersion,
        String model,
        String failureReason,
        Instant createdAt,
        List<GeneratedProjectResponse> projects
) {

    public static ResumeGenerationResponse from(ResumeGeneration generation) {
        return new ResumeGenerationResponse(
                generation.getId(),
                generation.getCandidateName(),
                generation.getPrimaryRole(),
                generation.getTotalExperienceYears(),
                generation.getStatus(),
                generation.getGeneratedSummary(),
                generation.getPromptVersion(),
                generation.getModel(),
                generation.getFailureReason(),
                generation.getCreatedAt(),
                generation.getProjects().stream().map(GeneratedProjectResponse::from).toList()
        );
    }
}
