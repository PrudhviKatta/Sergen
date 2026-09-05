package com.company.resumeai.prompt;

/**
 * §16 prompt builder for a single project section. Version string is
 * recorded on every GeneratedProject row (§33 prompt versioning) so a bad
 * batch of output can be traced back to the exact prompt that produced it —
 * bump VERSION (and add a v2 builder or branch) rather than silently editing
 * the wording an already-generated resume was produced with.
 */
public final class ProjectGenerationPromptBuilder {

    public static final String VERSION = "project-generation-v1";

    private static final String SYSTEM_PROMPT = """
            You generate professional software engineering resume content.

            Use the supplied internal examples only as contextual references. Do not copy sentences \
            from them verbatim. Do not claim technologies outside the approved list. Do not invent \
            awards, metrics, team sizes, certifications, or business outcomes. When information is \
            inferred rather than confirmed, prefer generic but realistic wording. Generate varied, \
            original wording - avoid phrasing that mirrors the reference patterns too closely.

            Respond with ONLY a single JSON object, no surrounding text, of the form:
            {"description": "one paragraph summarizing the project", \
            "responsibilities": ["bullet one", "bullet two", "..."], \
            "environment": ["technology one", "technology two", "..."]}
            """;

    private ProjectGenerationPromptBuilder() {
    }

    public static PromptMessages build(ProjectGenerationContext context) {
        StringBuilder user = new StringBuilder();
        user.append("INPUT PROJECT:\n");
        user.append("Client: ").append(context.client()).append('\n');
        user.append("Timeline: ").append(context.startDate()).append(" to ").append(context.endDate()).append('\n');
        user.append("Role: ").append(context.candidateRole()).append('\n');

        if (context.domain() != null && !context.domain().isBlank()) {
            user.append("\nDOMAIN CONTEXT:\n").append(context.domain()).append('\n');
        }

        user.append("\nAPPROVED TECHNOLOGIES:\n");
        for (String technology : context.approvedTechnologies()) {
            user.append(technology).append('\n');
        }

        if (!context.referenceSnippets().isEmpty()) {
            user.append("\nREFERENCE PROJECT PATTERNS:\n");
            for (String snippet : context.referenceSnippets()) {
                user.append("- ").append(snippet).append('\n');
            }
        }

        return new PromptMessages(SYSTEM_PROMPT, user.toString());
    }
}
