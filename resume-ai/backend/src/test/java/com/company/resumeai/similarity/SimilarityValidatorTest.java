package com.company.resumeai.similarity;

import com.company.resumeai.embedding.EmbeddingClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a hand-built EmbeddingClient (not a real/fake network call - just a
 * lambda mapping known strings to known vectors) so the exact cosine
 * similarity produced is under the test's control, unlike the
 * hash-of-content fakes used in the *IT tests.
 */
class SimilarityValidatorTest {

    private static final float[] IDENTICAL_VECTOR = {1f, 0f, 0f};
    private static final float[] ORTHOGONAL_VECTOR = {0f, 1f, 0f};
    private static final float[] CLOSE_VECTOR = {0.6f, 0.8f, 0f}; // unit vector, cosine 0.6 vs IDENTICAL_VECTOR

    @Test
    void acceptableWhenSemanticSimilarityBelowReviewThreshold() {
        SimilarityValidator validator = new SimilarityValidator(fixedVectorClient("reference", ORTHOGONAL_VECTOR, IDENTICAL_VECTOR));

        SimilarityCheckResult result = validator.evaluate("candidate", List.of("reference"));

        assertThat(result.semanticSimilarity()).isEqualTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(result.verdict()).isEqualTo(SimilarityVerdict.ACCEPTABLE);
        assertThat(result.duplicatePhraseDetected()).isFalse();
    }

    @Test
    void reviewWhenSemanticSimilarityBetweenThresholds() {
        SimilarityValidator validator = new SimilarityValidator(fixedVectorClient("reference", CLOSE_VECTOR, IDENTICAL_VECTOR));

        SimilarityCheckResult result = validator.evaluate("candidate", List.of("reference"));

        assertThat(result.semanticSimilarity()).isGreaterThanOrEqualTo(SimilarityValidator.REVIEW_THRESHOLD);
        assertThat(result.semanticSimilarity()).isLessThan(SimilarityValidator.REWRITE_THRESHOLD);
        assertThat(result.verdict()).isEqualTo(SimilarityVerdict.REVIEW);
    }

    @Test
    void rewriteWhenSemanticSimilarityAtOrAboveRewriteThreshold() {
        SimilarityValidator validator = new SimilarityValidator(fixedVectorClient("reference", IDENTICAL_VECTOR, IDENTICAL_VECTOR));

        SimilarityCheckResult result = validator.evaluate("candidate", List.of("reference"));

        assertThat(result.semanticSimilarity()).isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(result.verdict()).isEqualTo(SimilarityVerdict.REWRITE);
    }

    @Test
    void rewriteWhenDuplicatePhraseDetectedEvenIfSemanticSimilarityIsLow() {
        // Different full texts (so the embedding mock can give them genuinely
        // orthogonal vectors, unlike identical strings which no embedding model
        // could ever score as dissimilar) that nonetheless share a 12+ word run.
        String sharedPhrase = "Modernized a legacy banking platform by implementing microservices architecture using Java and Spring Boot";
        String referenceText = sharedPhrase + " for a retail company unrelated to telecom.";
        String candidateText = "Prior work: " + sharedPhrase + " for a large telecom enterprise rollout.";
        float[] referenceVector = {0f, 1f, 0f};
        float[] candidateVector = {0f, 0f, 1f}; // orthogonal to referenceVector -> similarity 0.0
        SimilarityValidator validator = new SimilarityValidator(
                text -> text.equals(referenceText) ? referenceVector : candidateVector);

        SimilarityCheckResult result = validator.evaluate(candidateText, List.of(referenceText));

        assertThat(result.semanticSimilarity()).isEqualTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(result.duplicatePhraseDetected()).isTrue();
        assertThat(result.verdict()).isEqualTo(SimilarityVerdict.REWRITE);
    }

    @Test
    void acceptableWithNoReferenceTextsAndNoEmbeddingCallNeeded() {
        SimilarityValidator validator = new SimilarityValidator(text -> {
            throw new AssertionError("embed() should not be called with no reference texts");
        });

        SimilarityCheckResult result = validator.evaluate("candidate", List.of());

        assertThat(result.semanticSimilarity()).isEqualTo(0.0);
        assertThat(result.verdict()).isEqualTo(SimilarityVerdict.ACCEPTABLE);
    }

    private EmbeddingClient fixedVectorClient(String referenceText, float[] referenceVector, float[] candidateVector) {
        return text -> text.equals(referenceText) ? referenceVector : candidateVector;
    }
}
