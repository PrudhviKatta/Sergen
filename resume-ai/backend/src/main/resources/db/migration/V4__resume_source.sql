-- Milestone 2 schema. §8.6 resume_source: raw text + LLM-parsed structured
-- JSON for one uploaded resume file. candidate_id is nullable - most uploads
-- are bulk historical resumes for the knowledge base (§45's "30 curated Java
-- resumes"), not tied to any specific candidate profile being generated for.
--
-- knowledge_fragment.source_resume_id and candidate_project.source_resume_id
-- were both created back in V1 as plain UUID columns with no FK, annotated
-- "FK added in Milestone 2 once resume_source exists" - that's now true, so
-- both get the constraint added here rather than staying untracked FKs.

CREATE TABLE resume_source (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id      UUID REFERENCES candidate(id) ON DELETE CASCADE,
    file_name         VARCHAR(255) NOT NULL,
    file_type         VARCHAR(20) NOT NULL,
    raw_text          TEXT,
    parsed_json       TEXT,
    ingestion_status  VARCHAR(20) NOT NULL,
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resume_source_candidate_id ON resume_source(candidate_id);

ALTER TABLE knowledge_fragment
    ADD CONSTRAINT fk_knowledge_fragment_source_resume
    FOREIGN KEY (source_resume_id) REFERENCES resume_source(id) ON DELETE SET NULL;

ALTER TABLE candidate_project
    ADD CONSTRAINT fk_candidate_project_source_resume
    FOREIGN KEY (source_resume_id) REFERENCES resume_source(id) ON DELETE SET NULL;
