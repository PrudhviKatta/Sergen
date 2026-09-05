package com.company.resumeai.prompt;

import java.util.List;

public record SummaryGenerationContext(
        String candidateName,
        String primaryRole,
        Integer totalExperienceYears,
        List<String> projectDescriptions
) {
}
