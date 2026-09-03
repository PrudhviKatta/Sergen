package com.company.resumeai.validation;

import com.company.resumeai.technology.EraProfile;
import com.company.resumeai.technology.EraProfileRepository;
import com.company.resumeai.technology.Technology;
import com.company.resumeai.technology.TechnologyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * §14 Technology Timeline Engine. Answers two questions for the (future,
 * Milestone 5) generation pipeline:
 *   1. {@link #check} - is this technology era-appropriate for a project's
 *      date range?
 *   2. {@link #suggestAlternatives} - given just a date range, which
 *      technologies (§40 era profiles) would be appropriate?
 *
 * Not wired into any REST endpoint yet - nothing consumes it until the
 * generation pipeline (Milestone 5) exists. Covered by
 * TechnologyTimelineValidatorTest (pure logic, no Spring context) and
 * TechnologyTimelineValidatorIT (real seeded catalog via V2 migration).
 */
@Service
public class TechnologyTimelineValidator {

    private final TechnologyRepository technologyRepository;
    private final EraProfileRepository eraProfileRepository;

    public TechnologyTimelineValidator(TechnologyRepository technologyRepository,
                                        EraProfileRepository eraProfileRepository) {
        this.technologyRepository = technologyRepository;
        this.eraProfileRepository = eraProfileRepository;
    }

    @Transactional(readOnly = true)
    public TechnologyTimelineCheck check(String technologyName, int projectStartYear, int projectEndYear) {
        Optional<Technology> technology = technologyRepository.findByName(technologyName);
        if (technology.isEmpty()) {
            return new TechnologyTimelineCheck(technologyName, TimelineStatus.UNKNOWN,
                    "not in the technology catalog - cannot verify");
        }
        return evaluate(technologyName, technology.get(), projectStartYear, projectEndYear);
    }

    @Transactional(readOnly = true)
    public List<TechnologyTimelineCheck> checkAll(Collection<String> technologyNames,
                                                    int projectStartYear, int projectEndYear) {
        return technologyNames.stream()
                .map(name -> check(name, projectStartYear, projectEndYear))
                .toList();
    }

    /**
     * Union of every era profile's technologies whose date range overlaps the
     * given project range, alphabetized. A project spanning multiple eras
     * (e.g. 2014-2017) legitimately draws from more than one profile.
     */
    @Transactional(readOnly = true)
    public List<String> suggestAlternatives(int projectStartYear, int projectEndYear) {
        TreeSet<String> names = new TreeSet<>();
        for (EraProfile profile : eraProfileRepository.findAll()) {
            boolean overlaps = profile.getStartYear() <= projectEndYear
                    && (profile.getEndYear() == null || profile.getEndYear() >= projectStartYear);
            if (overlaps) {
                profile.getTechnologies().forEach(t -> names.add(t.getName()));
            }
        }
        return List.copyOf(names);
    }

    private TechnologyTimelineCheck evaluate(String technologyName, Technology technology,
                                              int startYear, int endYear) {
        Short first = technology.getFirstAvailableYear();
        Short mainstream = technology.getMainstreamFromYear();
        Short deprecated = technology.getDeprecatedFromYear();

        if (deprecated != null && startYear >= deprecated) {
            return fail(technologyName, "deprecated from " + deprecated
                    + ", on or before this project's start year " + startYear);
        }
        if (first != null && endYear < first) {
            return fail(technologyName, "not released until " + first
                    + ", after this project's end year " + endYear);
        }
        if (first != null && startYear < first) {
            return questionable(technologyName, "released in " + first
                    + ", partway through a project starting " + startYear
                    + " - plausible only if adopted later in the engagement");
        }
        if (deprecated != null && endYear >= deprecated) {
            return questionable(technologyName, "deprecated from " + deprecated
                    + ", partway through a project ending " + endYear);
        }
        if (mainstream != null && startYear < mainstream) {
            return questionable(technologyName, "available from " + first + " but not mainstream until "
                    + mainstream + " - early/unusual adoption for a project starting " + startYear);
        }
        return new TechnologyTimelineCheck(technologyName, TimelineStatus.PASS, "within its mainstream window");
    }

    private TechnologyTimelineCheck fail(String technologyName, String reason) {
        return new TechnologyTimelineCheck(technologyName, TimelineStatus.FAIL, reason);
    }

    private TechnologyTimelineCheck questionable(String technologyName, String reason) {
        return new TechnologyTimelineCheck(technologyName, TimelineStatus.QUESTIONABLE, reason);
    }
}
