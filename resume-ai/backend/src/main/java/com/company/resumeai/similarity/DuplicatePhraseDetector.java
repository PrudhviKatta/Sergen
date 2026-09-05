package com.company.resumeai.similarity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * §18's text-similarity check: "if 12+ consecutive words match an existing
 * resume, flag the bullet." Implemented as a word-shingle intersection
 * (candidate's 12-word windows vs. each reference text's 12-word windows) -
 * simpler and dependency-free compared to a general sequence-matching /
 * longest-common-subsequence library, and exact enough for this rule's own
 * wording ("12+ consecutive words"), which is itself an exact-match rule,
 * not a fuzzy one.
 */
public final class DuplicatePhraseDetector {

    public static final int MIN_MATCHING_WORDS = 12;

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-z0-9]+");

    private DuplicatePhraseDetector() {
    }

    /** @return the first matching 12+ word phrase found, or empty if no reference text shares one. */
    public static Optional<String> findDuplicatePhrase(String candidateText, Collection<String> referenceTexts) {
        List<String> candidateWords = tokenize(candidateText);
        if (candidateWords.size() < MIN_MATCHING_WORDS) {
            return Optional.empty();
        }
        Set<String> candidateShingles = shingles(candidateWords);

        for (String referenceText : referenceTexts) {
            if (referenceText == null || referenceText.isBlank()) {
                continue;
            }
            List<String> referenceWords = tokenize(referenceText);
            if (referenceWords.size() < MIN_MATCHING_WORDS) {
                continue;
            }
            for (String shingle : shingles(referenceWords)) {
                if (candidateShingles.contains(shingle)) {
                    return Optional.of(shingle);
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> tokenize(String text) {
        return List.of(WORD_SPLIT.split(text.toLowerCase())).stream()
                .filter(word -> !word.isBlank())
                .toList();
    }

    private static Set<String> shingles(List<String> words) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i <= words.size() - MIN_MATCHING_WORDS; i++) {
            result.add(String.join(" ", words.subList(i, i + MIN_MATCHING_WORDS)));
        }
        return result;
    }
}
