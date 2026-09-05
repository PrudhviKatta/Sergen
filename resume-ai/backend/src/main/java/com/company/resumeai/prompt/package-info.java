/**
 * Versioned prompt templates (§33).
 *
 * Built (Milestone 5): {@link com.company.resumeai.prompt.ProjectGenerationPromptBuilder}
 * and {@link com.company.resumeai.prompt.SummaryGenerationPromptBuilder}, each with a
 * {@code VERSION} constant recorded on the generated row it produced (§33). Deliberately
 * plain Java text blocks, not files/DB templates as §33 suggests for the long run - that
 * adds a template-loading mechanism this project doesn't need yet with only two prompts.
 * Revisit if/when a v2 of either prompt needs to exist alongside v1.
 */
package com.company.resumeai.prompt;
