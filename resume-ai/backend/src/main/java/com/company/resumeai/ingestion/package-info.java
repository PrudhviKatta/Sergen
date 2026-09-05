/**
 * Resume upload + ingestion orchestration (§9, §21 POST /api/v1/resumes/upload).
 *
 * Built (Milestone 2): {@link com.company.resumeai.ingestion.ResumeUploadService}
 * runs §9's flow - extract text (parser.ResumeTextExtractor), parse
 * (parser.ResumeParser), persist {@link com.company.resumeai.ingestion.ResumeSource}
 * either way (PARSED or FAILED, same "record the failure, don't 500" pattern as
 * generation.ResumeGenerationService), then create knowledge fragments + embeddings
 * from whatever was parsed (knowledge.KnowledgeFragmentService). Deliberately does NOT
 * auto-create candidate_project rows - §31 Screen 1's "Edit parsed projects"/"Confirm
 * technologies" imply that's a separate, human-gated step, not automatic on upload; not
 * built yet.
 */
package com.company.resumeai.ingestion;
