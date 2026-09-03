package com.company.resumeai.validation;

import com.company.resumeai.common.exception.InvalidRequestException;

import java.time.LocalDate;

/**
 * §29 Chronology Validation. Was inline in project.CandidateProjectService for
 * Milestone 1 (see package-info); moved here now that this package exists, so
 * the rule has one home instead of being reimplemented per call site.
 */
public final class ChronologyValidator {

    private ChronologyValidator() {
    }

    public static void requireStartNotAfterEnd(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidRequestException("startDate must not be after endDate");
        }
    }
}
