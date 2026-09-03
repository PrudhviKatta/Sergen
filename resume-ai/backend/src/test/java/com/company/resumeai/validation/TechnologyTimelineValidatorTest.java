package com.company.resumeai.validation;

import com.company.resumeai.technology.EraProfileRepository;
import com.company.resumeai.technology.Technology;
import com.company.resumeai.technology.TechnologyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pure logic tests for the §14 timeline engine's PASS/QUESTIONABLE/FAIL/UNKNOWN
 * rules - repositories are mocked, no Spring context or database needed. See
 * TechnologyTimelineValidatorIT for a test against the real seeded catalog.
 */
@ExtendWith(MockitoExtension.class)
class TechnologyTimelineValidatorTest {

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private EraProfileRepository eraProfileRepository;

    private TechnologyTimelineValidator validator() {
        return new TechnologyTimelineValidator(technologyRepository, eraProfileRepository);
    }

    @Test
    void passesWhenWellWithinMainstreamWindow() {
        // Java 8: first=2014, mainstream=2014 - a 2015-2017 project is squarely inside it.
        stub("Java 8", 2014, 2014, null);

        TechnologyTimelineCheck result = validator().check("Java 8", 2015, 2017);

        assertThat(result.status()).isEqualTo(TimelineStatus.PASS);
    }

    @Test
    void failsWhenTechnologyDidNotExistYet() {
        // §14's own worked example: Java 21 (first=2023) on a 2011-2013 project.
        stub("Java 21", 2023, 2024, null);

        TechnologyTimelineCheck result = validator().check("Java 21", 2011, 2013);

        assertThat(result.status()).isEqualTo(TimelineStatus.FAIL);
        assertThat(result.reason()).contains("not released until 2023");
    }

    @Test
    void failsWhenProjectStartsAfterDeprecation() {
        // AngularJS deprecated from 2018; starting a NEW project in 2019 with it is a red flag.
        stub("AngularJS", 2010, 2012, 2018);

        TechnologyTimelineCheck result = validator().check("AngularJS", 2019, 2021);

        assertThat(result.status()).isEqualTo(TimelineStatus.FAIL);
        assertThat(result.reason()).contains("deprecated from 2018");
    }

    @Test
    void questionableWhenAdoptedBeforeMainstreamButAfterRelease() {
        // Spring Boot: first=2014, mainstream=2015 - §14 example: "Prefer use: 2015+".
        stub("Spring Boot", 2014, 2015, null);

        TechnologyTimelineCheck result = validator().check("Spring Boot", 2014, 2016);

        assertThat(result.status()).isEqualTo(TimelineStatus.QUESTIONABLE);
        assertThat(result.reason()).contains("not mainstream until 2015");
    }

    @Test
    void questionableWhenReleasedPartwayThroughProject() {
        stub("Some Framework", 2014, 2015, null);

        TechnologyTimelineCheck result = validator().check("Some Framework", 2013, 2015);

        assertThat(result.status()).isEqualTo(TimelineStatus.QUESTIONABLE);
        assertThat(result.reason()).contains("released in 2014");
    }

    @Test
    void questionableWhenDeprecatedPartwayThroughProject() {
        stub("SVN", 2000, 2004, 2018);

        TechnologyTimelineCheck result = validator().check("SVN", 2016, 2019);

        assertThat(result.status()).isEqualTo(TimelineStatus.QUESTIONABLE);
        assertThat(result.reason()).contains("deprecated from 2018");
    }

    @Test
    void unknownWhenNotInCatalog() {
        when(technologyRepository.findByName("Not A Real Framework")).thenReturn(Optional.empty());

        TechnologyTimelineCheck result = validator().check("Not A Real Framework", 2020, 2021);

        assertThat(result.status()).isEqualTo(TimelineStatus.UNKNOWN);
    }

    private void stub(String name, Integer firstAvailableYear, Integer mainstreamFromYear, Integer deprecatedFromYear) {
        Technology technology = new Technology(
                name,
                "test_category",
                firstAvailableYear == null ? null : firstAvailableYear.shortValue(),
                mainstreamFromYear == null ? null : mainstreamFromYear.shortValue(),
                deprecatedFromYear == null ? null : deprecatedFromYear.shortValue(),
                null
        );
        when(technologyRepository.findByName(name)).thenReturn(Optional.of(technology));
    }
}
