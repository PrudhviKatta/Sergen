package com.company.resumeai.similarity;

public record SimilarityCheckResult(
        double semanticSimilarity,
        boolean duplicatePhraseDetected,
        String duplicatePhrase,
        SimilarityVerdict verdict
) {
}
