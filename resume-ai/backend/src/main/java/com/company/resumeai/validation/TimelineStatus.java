package com.company.resumeai.validation;

/**
 * Result tier for {@link TechnologyTimelineValidator}. Deliberately three
 * tiers, not a binary pass/fail - §14's own worked example distinguishes
 * "Safe use" from "Prefer use" year thresholds for the same technology
 * (e.g. Spring Boot: released 2014, but "prefer 2015+"), which a strict
 * pass/fail can't represent without either being too strict or ignoring
 * the distinction entirely.
 */
public enum TimelineStatus {
    /** Within the technology's safe/mainstream window for this project's dates. */
    PASS,
    /** Technically possible but early/unusual adoption, or deprecated partway through. Flag for review, don't block. */
    QUESTIONABLE,
    /** Technology could not plausibly have been used at all during this project's dates. */
    FAIL,
    /** Technology name not found in the catalog - can't verify either way. */
    UNKNOWN
}
