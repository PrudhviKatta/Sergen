package com.company.resumeai.similarity;

/** §17's three uniqueness bands, applied to a semantic-similarity score (0.0-1.0), plus a hard override. */
public enum SimilarityVerdict {
    /** 0.00-0.55 - no action needed. */
    ACCEPTABLE,
    /** 0.55-0.70 - worth a human look, but not blocked. */
    REVIEW,
    /** 0.70+, or a §18 duplicate-phrase match regardless of score - must be rewritten. */
    REWRITE
}
