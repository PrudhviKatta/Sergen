package com.company.resumeai.parser;

import java.util.List;

/**
 * startDate/endDate are kept as the raw "YYYY-MM" strings the LLM extracts
 * (§10's own example shape), not parsed into LocalDate here - a resume's
 * dates are often incomplete or ambiguous ("2015 - Present", a season, a
 * year only), and forcing a strict date parse at extraction time would turn
 * "the model did its best" into a hard failure for the whole resume. Downstream
 * confirmation (turning this into a real candidate_project row) is where
 * strict date validation belongs - not built yet, see IMPLEMENTATION_NOTES.md.
 */
public record ParsedProject(
        String client,
        String role,
        String startDate,
        String endDate,
        String domain,
        List<String> technologies,
        List<String> responsibilities
) {
}
