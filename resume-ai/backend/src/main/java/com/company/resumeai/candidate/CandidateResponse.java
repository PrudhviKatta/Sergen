package com.company.resumeai.candidate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String primaryRole,
        BigDecimal totalExperienceYears,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {

    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getPrimaryRole(),
                candidate.getTotalExperienceYears(),
                candidate.getSummary(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
