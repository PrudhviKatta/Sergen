package com.company.resumeai.similarity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicatePhraseDetectorTest {

    @Test
    void findsExactTwelveWordMatchIgnoringCaseAndPunctuation() {
        String reference = "Modernized a legacy banking platform by implementing microservices architecture using Java and Spring Boot";
        String candidate = "Prior experience: MODERNIZED A LEGACY BANKING PLATFORM BY IMPLEMENTING MICROSERVICES ARCHITECTURE USING JAVA AND SPRING BOOT, delivered on time.";

        Optional<String> match = DuplicatePhraseDetector.findDuplicatePhrase(candidate, List.of(reference));

        assertThat(match).isPresent();
    }

    @Test
    void noMatchWhenFewerThanTwelveConsecutiveWordsShared() {
        String reference = "Modernized a legacy banking platform by implementing microservices architecture using Java and Spring Boot";
        String candidate = "Modernized a legacy banking platform for a different client using a completely different technology stack entirely.";

        Optional<String> match = DuplicatePhraseDetector.findDuplicatePhrase(candidate, List.of(reference));

        assertThat(match).isEmpty();
    }

    @Test
    void noMatchWhenCandidateShorterThanMinimumWindow() {
        Optional<String> match = DuplicatePhraseDetector.findDuplicatePhrase("Too short to match anything.",
                List.of("Modernized a legacy banking platform by implementing microservices architecture using Java and Spring Boot"));

        assertThat(match).isEmpty();
    }

    @Test
    void ignoresBlankAndNullReferenceTexts() {
        java.util.ArrayList<String> references = new java.util.ArrayList<>();
        references.add(null);
        references.add("   ");
        references.add("Some unrelated project narrative that does not share any real wording with the candidate.");

        Optional<String> match = DuplicatePhraseDetector.findDuplicatePhrase(
                "A completely independent description covering unrelated systems and unrelated clients in unrelated years.",
                references);

        assertThat(match).isEmpty();
    }
}
