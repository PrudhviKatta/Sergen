/**
 * Resume section parsing into structured JSON (§10, §11).
 *
 * Built (Milestone 2): {@link com.company.resumeai.parser.ResumeTextExtractor}
 * (PDF via PDFBox, DOCX via POI, plain text needs nothing) for §9's "Extract Text"
 * step, and {@link com.company.resumeai.parser.ResumeParser} (LLM-based, via
 * {@code llm.LlmClient} and {@code prompt.ResumeParsingPromptBuilder}) for §10's
 * structured extraction - rule-based section detection was rejected as
 * impractical for Phase 1 given how much resume layouts vary.
 * {@link com.company.resumeai.parser.ParsedResume} preserves §10's own shape
 * (candidate + projects, each project's technologies/responsibilities as
 * plain lists); dates are kept as raw strings, not parsed into LocalDate -
 * see ParsedProject's javadoc for why.
 *
 * Orchestration (upload -> extract -> parse -> persist -> create knowledge
 * fragments) lives in {@code ingestion.ResumeUploadService}, not here - this
 * package only knows how to turn bytes into structured data, not what to do
 * with the result.
 */
package com.company.resumeai.parser;
