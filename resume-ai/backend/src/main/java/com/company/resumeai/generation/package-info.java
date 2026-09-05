/**
 * LLM generation pipeline: prompt orchestration, resume assembly (§12, §16, §28).
 *
 * Built (Milestone 5): {@link com.company.resumeai.generation.ResumeGenerationService}
 * orchestrating §12's pipeline: for each input project, resolve era-appropriate technologies
 * (validation.TechnologyTimelineValidator), retrieve reference fragments
 * (retrieval.RetrievalService), build a prompt (prompt.ProjectGenerationPromptBuilder),
 * call the LLM, then generate one final candidate-summary call
 * (prompt.SummaryGenerationPromptBuilder) across every project's output.
 * {@link com.company.resumeai.generation.ResumeGeneration} /
 * {@link com.company.resumeai.generation.GeneratedProject} persist the result either way
 * (COMPLETED or FAILED - a failed LLM call is recorded, not thrown as a 500).
 *
 * Built (Milestone 6): each project draft is scored by
 * similarity.SimilarityValidator against retrieval reference snippets plus any
 * sibling projects already drafted in this same request; a REWRITE verdict
 * (§17/§18) regenerates the draft, up to a hardcoded MAX_REWRITE_ATTEMPTS (§19).
 * The final score/verdict and how many attempts it took are recorded on
 * GeneratedProject.
 *
 * Not yet built: regenerate/approve/export endpoints (§21 - tied to export,
 * Milestone 8, and to a persisted per-project review flow that doesn't exist yet),
 * and the LLM-as-judge tone/quality validator (§29) - not part of Milestone 6's
 * own deliverables list.
 */
package com.company.resumeai.generation;
