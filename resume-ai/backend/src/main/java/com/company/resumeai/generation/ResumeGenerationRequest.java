package com.company.resumeai.generation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** §11 generation input / §21 "Generate Base Resume" request body. */
public record ResumeGenerationRequest(
        @NotBlank String candidateName,
        @NotBlank String primaryRole,
        Integer totalExperienceYears,
        @NotEmpty @Valid List<ProjectGenerationInput> projects
) {
}
