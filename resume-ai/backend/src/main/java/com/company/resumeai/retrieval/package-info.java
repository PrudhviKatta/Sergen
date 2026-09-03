/**
 * Hybrid structured-filter + vector retrieval engine (§13).
 *
 * Built (Milestone 3): {@link com.company.resumeai.retrieval.RetrievalService}
 * combines §13 step 1 (structured filters: domain, role, client, year range)
 * with step 2 (pgvector cosine-distance ranking) in one native query on
 * KnowledgeFragmentRepository. §13 step 3 (diversity selection - don't return
 * near-duplicate fragments from the same source) is NOT implemented here;
 * it's deferred to the generation pipeline (Milestone 5) that will actually
 * consume these results.
 */
package com.company.resumeai.retrieval;
