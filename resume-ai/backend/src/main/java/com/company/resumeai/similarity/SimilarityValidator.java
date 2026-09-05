package com.company.resumeai.similarity;

import com.company.resumeai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * §17/§18: scores a freshly generated text against a set of reference texts
 * (existing project summaries, sibling projects already generated in this
 * same request - see generation.ResumeGenerationService for what it passes
 * in). Two independent checks, either of which can force a REWRITE verdict:
 * semantic similarity (embedding cosine similarity, §17's threshold bands)
 * and an exact duplicate-phrase match (§18, DuplicatePhraseDetector).
 *
 * Deliberately re-embeds every reference text on every call rather than
 * caching/reusing a KnowledgeFragment's already-stored embedding - keeps this
 * service simple and provider-agnostic (it only knows "text in, vector out",
 * not where a text came from). Acceptable for Phase 1's data volumes; revisit
 * if embedding-call volume becomes a real cost concern.
 */
@Service
public class SimilarityValidator {

    public static final double REVIEW_THRESHOLD = 0.55;
    public static final double REWRITE_THRESHOLD = 0.70;

    private final EmbeddingClient embeddingClient;

    public SimilarityValidator(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public SimilarityCheckResult evaluate(String candidateText, List<String> referenceTexts) {
        List<String> nonBlankReferences = referenceTexts.stream().filter(t -> t != null && !t.isBlank()).toList();

        double semanticSimilarity = 0.0;
        if (!nonBlankReferences.isEmpty()) {
            float[] candidateVector = embeddingClient.embed(candidateText);
            for (String referenceText : nonBlankReferences) {
                float[] referenceVector = embeddingClient.embed(referenceText);
                semanticSimilarity = Math.max(semanticSimilarity, cosineSimilarity(candidateVector, referenceVector));
            }
        }

        Optional<String> duplicatePhrase = DuplicatePhraseDetector.findDuplicatePhrase(candidateText, nonBlankReferences);
        SimilarityVerdict verdict = classify(semanticSimilarity, duplicatePhrase.isPresent());

        return new SimilarityCheckResult(semanticSimilarity, duplicatePhrase.isPresent(), duplicatePhrase.orElse(null), verdict);
    }

    static SimilarityVerdict classify(double semanticSimilarity, boolean duplicatePhraseDetected) {
        if (duplicatePhraseDetected || semanticSimilarity >= REWRITE_THRESHOLD) {
            return SimilarityVerdict.REWRITE;
        }
        if (semanticSimilarity >= REVIEW_THRESHOLD) {
            return SimilarityVerdict.REVIEW;
        }
        return SimilarityVerdict.ACCEPTABLE;
    }

    static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
