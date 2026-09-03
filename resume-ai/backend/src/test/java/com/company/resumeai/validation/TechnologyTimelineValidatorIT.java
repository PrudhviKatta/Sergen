package com.company.resumeai.validation;

import com.company.resumeai.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises TechnologyTimelineValidator against the real catalog seeded by
 * V2__era_profiles_and_technology_seed.sql - confirms the migration data and
 * the validator logic agree, not just the logic in isolation
 * (TechnologyTimelineValidatorTest covers that with mocks).
 */
class TechnologyTimelineValidatorIT extends AbstractIntegrationTest {

    @Autowired
    private TechnologyTimelineValidator validator;

    @Test
    void flagsSeededTechnologyAsFailWhenTooNewForProject() {
        // §14's own worked example.
        TechnologyTimelineCheck result = validator.check("Java 21", 2011, 2013);

        assertThat(result.status()).isEqualTo(TimelineStatus.FAIL);
    }

    @Test
    void passesSeededTechnologyWithinItsMainstreamWindow() {
        TechnologyTimelineCheck result = validator.check("Spring Boot", 2016, 2019);

        assertThat(result.status()).isEqualTo(TimelineStatus.PASS);
    }

    @Test
    void unknownForATechnologyNotInTheSeededCatalog() {
        TechnologyTimelineCheck result = validator.check("Some Made Up Framework", 2016, 2019);

        assertThat(result.status()).isEqualTo(TimelineStatus.UNKNOWN);
    }

    @Test
    void suggestsAlternativesSpanningOverlappingEraProfiles() {
        // 2011-2013 overlaps both the 2008-2011 and 2012-2015 era profiles.
        List<String> suggestions = validator.suggestAlternatives(2011, 2013);

        assertThat(suggestions).contains("Struts", "Oracle", "Java 7", "AWS");
        assertThat(suggestions).doesNotContain("Kubernetes", "Java 21", "Spring Boot 3");
    }
}
