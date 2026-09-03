package com.company.resumeai.project;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectCreateRequest(
        @NotNull UUID clientId,
        String projectName,
        String roleTitle,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String domain,
        String projectSummary
) {
}
