# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`resume-ai` (repo: Sergen) — Phase 1 of an AI-assisted resume generator, built
against `resume-ai/docs/PHASE1_BASE_RESUME_GENERATOR.md`, the source-of-truth
spec (referenced below as `§N`). Current build status, every non-obvious
design decision, and what's next live in `resume-ai/docs/IMPLEMENTATION_NOTES.md`
— **read it before making changes**; this file is deliberately just commands
and architecture, not a running log.

## Commands

All commands run from `resume-ai/backend/`. Requires Java 21 on `JAVA_HOME`
and a running Docker daemon (integration tests use Testcontainers).

```bash
# from resume-ai/: start Postgres+pgvector only, then run the app against it
docker compose up -d postgres
cd backend && DB_HOST=localhost mvn spring-boot:run

# compile only
mvn -DskipTests package

# full suite: unit (Surefire) + Testcontainers integration (Failsafe)
mvn verify

# a single unit test class/method (Surefire only, *Test.java)
mvn -Dtest=VectorCodecTest test
mvn -Dtest=VectorCodecTest#roundTripsThroughEncodeAndDecode test

# a single integration test class/method (Failsafe, *IT.java — needs `verify`, not `test`)
mvn verify -Dit.test=CandidateApiIT -DfailIfNoTests=false
mvn verify -Dit.test=CandidateApiIT#createAndFetchCandidate -DfailIfNoTests=false

# everything in containers (from resume-ai/)
docker compose up --build
```

CI runs `mvn verify` on every push/PR to `main` and `dev`
(`.github/workflows/ci.yml`).

## Architecture

Modular monolith (§52) — one Spring Boot app, packages by domain concept
under `com.company.resumeai`. Every top-level package maps to a milestone in
the spec, including the still-empty ones (`ingestion`, `parser`, `generation`,
`prompt`, `similarity`, `export`, `audit`, `config`) — each has a
`package-info.java` stating which milestone owns it and which spec section it
implements. Check there before creating a new top-level package; the slot is
probably already reserved.

Layering is consistent across `candidate`/`client`/`project`/`knowledge`:
Entity → Repository (Spring Data JPA) → Service (`@Transactional`, business
rules) → Controller (DTOs in/out, entities never cross that boundary). DTOs
are Java records; entities are hand-written getters/constructors, no Lombok.

Schema is Flyway-owned — `spring.jpa.hibernate.ddl-auto: validate`, Hibernate
only checks entity mappings agree with what Flyway created, it never
generates DDL. Migrations live in `backend/src/main/resources/db/migration/`.
`V1`/`V2` are already applied to every environment that's run them — never
edit an applied migration, add `V3`, `V4`, etc.

Key cross-cutting pieces:

- `common.web.GlobalExceptionHandler` — one `@RestControllerAdvice` funnels
  `ResourceNotFoundException` (404), `InvalidRequestException` (400),
  bean-validation failures (400, `VALIDATION_FAILED` error code), and
  anything else (500) into one `ApiError` JSON shape.
- `validation.TechnologyTimelineValidator` — date-math against the seeded
  `technology`/`era_profile` tables (PASS/QUESTIONABLE/FAIL/UNKNOWN), no LLM
  involved. Not wired to any endpoint yet — internal only until Milestone 5.
- `embedding.EmbeddingClient` (→ `OpenAiEmbeddingClient`) and
  `retrieval.RetrievalService` — OpenAI embeddings plus a native pgvector
  query (`KnowledgeFragmentRepository.findSimilar`; Spring Data can't express
  the `<=>` operator declaratively). `knowledge.KnowledgeFragment.embedding`
  is a `String` field with Hibernate `@ColumnTransformer` casting it to/from
  the real `vector(1536)` column — see `embedding.VectorCodec` for the
  `float[]` ↔ text conversion.

Testing: `AbstractIntegrationTest` starts **one** Testcontainers
Postgres+pgvector for the entire test JVM (manual `static { POSTGRES.start(); }`,
deliberately not `@Testcontainers`/`@Container` — that annotation pair tears
the container down per test class, which breaks once the field is inherited
by multiple subclasses). Consequence: every `*IT` class shares one database
with no per-test rollback, so **fixture data (emails, domain values, etc.)
must be unique across the whole test suite, not just within one test
method** — a value that collides with something another test inserted will
fail non-deterministically depending on run order.

## Config

`OPENAI_API_KEY` is required only for code paths touching
`embedding.EmbeddingClient` (currently `POST /api/v1/knowledge-fragments` and
its search endpoint) — everything else runs with zero external config.
`DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` override the Postgres
connection; defaults match `docker-compose.yml`.
