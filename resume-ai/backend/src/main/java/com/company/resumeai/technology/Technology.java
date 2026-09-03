package com.company.resumeai.technology;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Catalog row for the §14 technology timeline engine. Populated by
 * V2__era_profiles_and_technology_seed.sql, queried by
 * validation.TechnologyTimelineValidator. No dedicated REST controller -
 * nothing external needs to read/write this table directly yet; it's
 * consumed internally until the generation pipeline (Milestone 5) exists.
 */
@Entity
@Table(name = "technology")
public class Technology {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "first_available_year")
    private Short firstAvailableYear;

    @Column(name = "mainstream_from_year")
    private Short mainstreamFromYear;

    @Column(name = "deprecated_from_year")
    private Short deprecatedFromYear;

    @Column(name = "notes")
    private String notes;

    protected Technology() {
        // JPA
    }

    public Technology(String name, String category, Short firstAvailableYear,
                       Short mainstreamFromYear, Short deprecatedFromYear, String notes) {
        this.name = name;
        this.category = category;
        this.firstAvailableYear = firstAvailableYear;
        this.mainstreamFromYear = mainstreamFromYear;
        this.deprecatedFromYear = deprecatedFromYear;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Short getFirstAvailableYear() {
        return firstAvailableYear;
    }

    public Short getMainstreamFromYear() {
        return mainstreamFromYear;
    }

    public Short getDeprecatedFromYear() {
        return deprecatedFromYear;
    }

    public String getNotes() {
        return notes;
    }
}
