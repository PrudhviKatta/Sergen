-- Milestone 1 schema. Covers §8.1, §8.2, §8.3, §8.5, §8.7 of
-- PHASE1_BASE_RESUME_GENERATOR.md.
--
-- Deliberately NOT included yet (future milestones):
--   - project_technology join table (§8.4)         -> Milestone 4 (technology timeline engine)
--   - resume_source table (§8.6)                    -> Milestone 2 (resume ingestion)
--   - knowledge_fragment.embedding is created here as a vector column so the table
--     shape matches the spec, but nothing writes to it yet, and no ivfflat/hnsw index
--     is added until Milestone 3 defines the actual embedding dimension/provider.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE candidate (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    primary_role            VARCHAR(150),
    total_experience_years  NUMERIC(4,1),
    summary                 TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_candidate_email UNIQUE (email)
);

CREATE TABLE client (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    normalized_name  VARCHAR(200) NOT NULL,
    industry         VARCHAR(150),
    description      TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_client_normalized_name UNIQUE (normalized_name)
);

CREATE TABLE candidate_project (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id      UUID NOT NULL REFERENCES candidate(id) ON DELETE CASCADE,
    client_id         UUID NOT NULL REFERENCES client(id),
    project_name      VARCHAR(200),
    role_title        VARCHAR(150),
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    domain            VARCHAR(150),
    project_summary   TEXT,
    source_resume_id  UUID,                 -- FK added in Milestone 2 once resume_source exists
    confidence_score  NUMERIC(3,2),          -- 0.00-1.00, see §30 confidence model
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_candidate_project_dates CHECK (start_date <= end_date)
);

CREATE INDEX idx_candidate_project_candidate_id ON candidate_project(candidate_id);
CREATE INDEX idx_candidate_project_client_id ON candidate_project(client_id);

CREATE TABLE technology (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(150) NOT NULL,
    category               VARCHAR(100),
    first_available_year   SMALLINT,
    mainstream_from_year   SMALLINT,
    deprecated_from_year   SMALLINT,
    notes                  TEXT,
    CONSTRAINT uq_technology_name UNIQUE (name)
);

CREATE TABLE knowledge_fragment (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id      UUID REFERENCES candidate(id) ON DELETE CASCADE,
    client_id         UUID REFERENCES client(id),
    project_id        UUID REFERENCES candidate_project(id) ON DELETE CASCADE,
    fragment_type     VARCHAR(50) NOT NULL,
    content           TEXT NOT NULL,
    domain            VARCHAR(150),
    role              VARCHAR(150),
    start_year        SMALLINT,
    end_year          SMALLINT,
    embedding         vector(1536),          -- placeholder dim; confirm against Milestone 3 embedding model
    source_resume_id  UUID,                  -- FK added in Milestone 2 once resume_source exists
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_fragment_candidate_id ON knowledge_fragment(candidate_id);
CREATE INDEX idx_knowledge_fragment_client_id ON knowledge_fragment(client_id);
CREATE INDEX idx_knowledge_fragment_project_id ON knowledge_fragment(project_id);
