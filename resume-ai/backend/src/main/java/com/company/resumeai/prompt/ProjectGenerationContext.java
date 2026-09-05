package com.company.resumeai.prompt;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything {@link ProjectGenerationPromptBuilder} needs, already resolved
 * by the caller (generation.ResumeGenerationService) — this package only
 * knows how to turn plain values into prompt text, not how to fetch them
 * (retrieval, the technology timeline engine, etc).
 */
public record ProjectGenerationContext(
        String candidateRole,
        String client,
        LocalDate startDate,
        LocalDate endDate,
        String domain,
        List<String> approvedTechnologies,
        List<String> referenceSnippets
) {
}
