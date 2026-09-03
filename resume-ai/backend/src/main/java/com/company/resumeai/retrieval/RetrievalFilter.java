package com.company.resumeai.retrieval;

import java.util.UUID;

/**
 * §13 step 1 structured filters. All fields optional - null means "don't
 * filter on this". startYear/endYear are matched as a range overlap against
 * a fragment's own start_year/end_year, not exact equality.
 */
public record RetrievalFilter(String domain, String role, UUID clientId, Integer startYear, Integer endYear) {

    public static RetrievalFilter none() {
        return new RetrievalFilter(null, null, null, null, null);
    }
}
