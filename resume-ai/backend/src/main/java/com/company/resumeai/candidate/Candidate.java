package com.company.resumeai.candidate;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate")
public class Candidate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "primary_role")
    private String primaryRole;

    @Column(name = "total_experience_years")
    private BigDecimal totalExperienceYears;

    @Column(name = "summary")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Candidate() {
        // JPA
    }

    public Candidate(String firstName, String lastName, String email, String primaryRole,
                      BigDecimal totalExperienceYears, String summary) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.primaryRole = primaryRole;
        this.totalExperienceYears = totalExperienceYears;
        this.summary = summary;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public BigDecimal getTotalExperienceYears() {
        return totalExperienceYears;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
