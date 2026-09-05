/**
 * Similarity/duplication checking and rewrite loop (§17, §18, §19).
 *
 * Built (Milestone 6): {@link com.company.resumeai.similarity.SimilarityValidator}
 * combines embedding cosine similarity (§17's ACCEPTABLE/REVIEW/REWRITE bands) with
 * {@link com.company.resumeai.similarity.DuplicatePhraseDetector}'s exact 12+-word
 * shingle match (§18) into one {@link com.company.resumeai.similarity.SimilarityCheckResult}.
 * The rewrite loop itself (§19, MAX_REWRITE_ATTEMPTS) lives in
 * generation.ResumeGenerationService, which is the actual caller/consumer of this
 * package - this package only scores a draft, it doesn't know how to regenerate one.
 *
 * Scope note: reference texts compared against are the retrieval snippets already
 * fetched for the prompt plus sibling projects generated in the *same* request. Checking
 * against previously generated resumes from *other*, earlier /resume-generations requests
 * (also listed in §18) is deferred - ResumeGeneration.candidateName is a free-text field,
 * not a link to a persistent candidate.Candidate, so "this candidate's other resumes"
 * isn't a query that can be asked yet. The tone/quality LLM-judge (§29) is also not part
 * of this package - Milestone 6's own deliverables list is embedding similarity +
 * duplicate-phrase check + rewrite loop only.
 */
package com.company.resumeai.similarity;
