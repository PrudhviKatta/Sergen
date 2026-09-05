package com.company.resumeai.prompt;

/**
 * §9/§10 resume parsing. Unlike ProjectGenerationPromptBuilder/SummaryGenerationPromptBuilder
 * (which generate new content), this prompt only extracts what's already present in the
 * resume text - the system prompt is explicit that inventing anything not in the source
 * text is wrong, not just undesirable, since a fabricated project/technology here would
 * silently corrupt the knowledge base rather than just one generated resume.
 */
public final class ResumeParsingPromptBuilder {

    // v2: added §20's non-project-specific sections (name/contact, professional
    // summary, overall technical skills, education, certifications) - v1 only
    // captured primaryRole/totalExperience/projects. Bumped rather than silently
    // widening v1's contract, per §33 - a resume parsed under v1 genuinely has a
    // different shape than one parsed under v2, and that's worth being able to
    // tell apart later.
    public static final String VERSION = "resume-parsing-v2";

    private static final String SYSTEM_PROMPT = """
            You extract structured data from resume text. Extract only what is explicitly \
            present in the text - do not infer, guess, or invent a name, client, role, date, \
            technology, responsibility, degree, or certification that is not actually stated. \
            If a field cannot be determined from the text, omit it or use null/an empty list \
            rather than guessing.

            Respond with ONLY a single JSON object, no surrounding text, of the form:
            {"candidate": {"firstName": "...", "lastName": "...", "email": "...", \
            "phone": "...", "location": "...", "primaryRole": "...", "totalExperience": 11, \
            "summary": "...", "technicalSkills": ["..."], "education": ["..."], \
            "certifications": ["..."]}, \
            "projects": [{"client": "...", "role": "...", "startDate": "YYYY-MM", \
            "endDate": "YYYY-MM", "domain": "...", "technologies": ["..."], \
            "responsibilities": ["..."]}]}

            "projects" should be empty if none can be identified. Dates should be extracted \
            in whatever form the resume states them (e.g. "2015-01", "2015", "Present") - do \
            not invent a specific month that isn't in the text. "technicalSkills" is the \
            candidate's overall/summary skill list if the resume has one (e.g. a "Technical \
            Skills" section) - separate from each project's own "technologies" list. \
            "education" and "certifications" should each be one string per entry (e.g. \
            "B.S. Computer Science, State University, 2013" or "AWS Certified Solutions \
            Architect, 2021") - do not split a single entry across multiple list items.
            """;

    private ResumeParsingPromptBuilder() {
    }

    public static PromptMessages build(String resumeText) {
        String user = "RESUME TEXT:\n" + resumeText;
        return new PromptMessages(SYSTEM_PROMPT, user);
    }
}
