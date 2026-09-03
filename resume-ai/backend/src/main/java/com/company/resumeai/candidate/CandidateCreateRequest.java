package com.company.resumeai.candidate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CandidateCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String primaryRole,
        @DecimalMin("0.0") @DecimalMax("80.0") BigDecimal totalExperienceYears,
        String summary
) {
}
