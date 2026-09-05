-- Milestone 5 schema. Stores the output of the generation pipeline (§12)
-- so a resume-generations run can be replayed/inspected later (§32
-- Auditability), even though most of §32's fields (retrieved fragment IDs,
-- similarity score, rewrite attempts, user edits, approval status) are not
-- yet captured — those land with Milestone 6 (similarity validator) and the
-- review UI (Milestone 7).

CREATE TABLE resume_generation (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_name          VARCHAR(200) NOT NULL,
    primary_role            VARCHAR(150) NOT NULL,
    total_experience_years  INTEGER,
    status                  VARCHAR(20) NOT NULL,
    generated_summary       TEXT,
    prompt_version          VARCHAR(50),
    model                   VARCHAR(100),
    failure_reason          TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE generated_project (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_generation_id    UUID NOT NULL REFERENCES resume_generation(id) ON DELETE CASCADE,
    client_name             VARCHAR(200) NOT NULL,
    role_title              VARCHAR(150),
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    domain                  VARCHAR(150),
    description             TEXT,
    responsibilities        TEXT,
    environment             VARCHAR(2000),
    prompt_version          VARCHAR(50),
    similarity_score        DOUBLE PRECISION,
    similarity_verdict      VARCHAR(20),
    duplicate_phrase_detected BOOLEAN NOT NULL DEFAULT FALSE,
    rewrite_attempts        INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_generated_project_dates CHECK (start_date <= end_date)
);

CREATE INDEX idx_generated_project_resume_generation_id ON generated_project(resume_generation_id);
