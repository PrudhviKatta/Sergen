package com.company.resumeai.prompt;

/** §16/§28 "Candidate Summary Generator" — the last task in the pipeline, run after every project is drafted. */
public final class SummaryGenerationPromptBuilder {

    public static final String VERSION = "summary-generation-v1";

    private static final String SYSTEM_PROMPT = """
            You write a concise professional summary for a software engineering resume, 2-4 sentences.

            Base it only on the project descriptions provided. Do not invent employers, certifications, \
            metrics, or achievements that are not present in those descriptions. Respond with plain text \
            only - no headings, no markdown, no JSON.
            """;

    private SummaryGenerationPromptBuilder() {
    }

    public static PromptMessages build(SummaryGenerationContext context) {
        StringBuilder user = new StringBuilder();
        user.append("Candidate: ").append(context.candidateName()).append('\n');
        user.append("Primary role: ").append(context.primaryRole()).append('\n');
        if (context.totalExperienceYears() != null) {
            user.append("Total experience: ").append(context.totalExperienceYears()).append(" years\n");
        }

        user.append("\nPROJECT DESCRIPTIONS:\n");
        int i = 1;
        for (String description : context.projectDescriptions()) {
            user.append(i++).append(". ").append(description).append('\n');
        }

        return new PromptMessages(SYSTEM_PROMPT, user.toString());
    }
}
