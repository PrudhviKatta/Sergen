package com.company.resumeai.technology;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity + repository only for Milestone 1, per §55/§56. No service/controller
 * yet - the technology catalog gets populated and queried starting in
 * Milestone 4 (technology timeline engine, §14).
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
