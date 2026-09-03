package com.company.resumeai.technology;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * §40 era profile: a curated, named set of technologies typical for a given
 * date range (e.g. "2016-2019" -> Spring Boot, Kafka, Angular/React, Docker...).
 * Deliberately curated data, not derived purely from
 * {@link Technology#getFirstAvailableYear()} math - "typical stack" is an
 * editorial judgment (see §40), technology-existence is a date fact.
 */
@Entity
@Table(name = "era_profile")
public class EraProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "label", nullable = false, unique = true)
    private String label;

    @Column(name = "start_year", nullable = false)
    private Short startYear;

    @Column(name = "end_year")
    private Short endYear; // null = open-ended (the "2024+" profile)

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "era_profile_technology",
            joinColumns = @JoinColumn(name = "era_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    private Set<Technology> technologies = new HashSet<>();

    protected EraProfile() {
        // JPA
    }

    public EraProfile(String label, Short startYear, Short endYear) {
        this.label = label;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public boolean coversYear(int year) {
        return year >= startYear && (endYear == null || year <= endYear);
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Short getStartYear() {
        return startYear;
    }

    public Short getEndYear() {
        return endYear;
    }

    public Set<Technology> getTechnologies() {
        return technologies;
    }
}
