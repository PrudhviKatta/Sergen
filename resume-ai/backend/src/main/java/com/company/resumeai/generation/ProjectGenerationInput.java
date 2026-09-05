package com.company.resumeai.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * §11 generation input, one project entry. `role`/`domain`/`knownTechnologies`
 * are §11's listed optional fields; when `knownTechnologies` is omitted, the
 * era-appropriate technology list comes entirely from
 * validation.TechnologyTimelineValidator#suggestAlternatives instead (§14).
 */
public record ProjectGenerationInput(
        @NotBlank String client,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String role,
        String domain,
        List<String> knownTechnologies
) {
}
