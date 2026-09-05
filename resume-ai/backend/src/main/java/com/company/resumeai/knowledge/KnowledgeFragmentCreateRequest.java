package com.company.resumeai.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KnowledgeFragmentCreateRequest(
        UUID candidateId,
        UUID clientId,
        UUID projectId,
        @NotNull FragmentType fragmentType,
        @NotBlank String content,
        String domain,
        String role,
        Short startYear,
        Short endYear,
        UUID sourceResumeId
) {
}
