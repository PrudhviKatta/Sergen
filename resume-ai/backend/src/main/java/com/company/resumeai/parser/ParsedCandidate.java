package com.company.resumeai.parser;

import java.util.List;

/**
 * §20's resume sections that aren't tied to a specific project: name/contact
 * info, professional summary, overall technical skills, education,
 * certifications - alongside §10's original primaryRole/totalExperience.
 * Field names mirror §8.1's candidate table where a direct mapping exists
 * (firstName/lastName/email/summary) - useful if a future "confirm" step
 * ever populates a real Candidate row from this.
 */
public record ParsedCandidate(
        String firstName,
        String lastName,
        String email,
        String phone,
        String location,
        String primaryRole,
        Integer totalExperienceYears,
        String summary,
        List<String> technicalSkills,
        List<String> education,
        List<String> certifications
) {
}
