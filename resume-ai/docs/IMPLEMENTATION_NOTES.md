# Implementation Notes — Milestone 1

Read `PHASE1_BASE_RESUME_GENERATOR.md` in this folder first; it is the source
of truth. Section references below (`§N`) point into that document. This file
tracks what has actually been built, why specific choices were made, and what
is still open — keep it up to date as later milestones land.

## Status: Milestone 1 complete and build-verified (2026-09-03)

`mvn -DskipTests package` and `mvn verify` both pass — 9/9 integration tests
green against a real Postgres+pgvector container (OrbStack, Java 21, Maven
3.9.16). Two real bugs turned up on the first verified run and are fixed on
`main`/`dev`; recorded here so nobody "fixes" them again from scratch or
reintroduces the same pattern elsewhere:

1. **`mvn verify` was silently running zero integration tests.** The `*IT.java`
   naming convention only means something to the Failsafe plugin, which
   `pom.xml` never declared — only `spring-boot-maven-plugin` was bound.
   Surefire (the plugin that *was* wired) ignores `*IT.java` by its default
   include pattern, so `verify` reported success having executed nothing.
   Fixed by adding `maven-failsafe-plugin` with `integration-test`+`verify`
   goals in `pom.xml`. **Lesson: a green `mvn verify` with zero test-count
   output printed is a red flag, not a pass — check the actual test count.**

2. **Testcontainers container got torn down between test classes.**
   `AbstractIntegrationTest` originally used `@Testcontainers` + `@Container`
   on a `static` `PostgreSQLContainer` field. Because the field lives on the
   shared base class (one field, shared identity across every subclass), the
   first test class to finish stopped the container in its `afterAll`, and
   the next class's cached Spring context kept the now-dead JDBC port —
   `Connection refused`. Fixed by switching to Testcontainers' documented
   "singleton container" pattern: start it manually in a `static { }`
   initializer, no `@Container` annotation, no explicit stop (the Ryuk
   reaper cleans it up when the JVM exits). This also made Spring reuse one
   cached `ApplicationContext` across all three IT classes instead of
   rebuilding it per class — full suite dropped from ~3 min to ~6s.

3. **`GET /candidates/{id}/projects` threw `LazyInitializationException`.**
   `ProjectResponse.from()` reads `project.getClient().getName()`, a lazy
   `@ManyToOne`. `POST .../projects` never hit this because the `Client` it
   maps is the same already-loaded managed instance handed in from
   `clientService.getById()`, not a lazy proxy — but the list query returns
   fresh entities where `candidate`/`client` genuinely are unfetched proxies,
   and `open-in-view` is off, so the session is closed by the time the
   controller maps them. Fixed with `@EntityGraph(attributePaths =
   {"candidate", "client"})` on `findByCandidateIdOrderByStartDateAsc`.
   **Lesson: a service method returning JPA entities with lazy associations
   is only as safe as every call site that touches those associations later
   — verify each one, don't assume "it worked for create" covers "list" too.**

To reproduce locally:

```
cd resume-ai/backend
mvn -DskipTests package   # compile + package
mvn verify                 # unit + Testcontainers integration tests, needs Docker running
```

## What's implemented

Directory: `resume-ai/backend/src/main/java/com/company/resumeai/`

| Package | Contents | Notes |
|---|---|---|
| `candidate` | `Candidate` entity, repository, service, controller, DTOs | Full CRUD-lite: create + get by id |
| `client` | `Client` entity, repository, service, controller, DTOs, `ClientNameNormalizer` | Create is upsert-by-normalized-name (see below) |
| `project` | `CandidateProject` entity, repository, service, controller, DTOs | Create + list-by-candidate; owns the chronology check |
| `technology` | `Technology` entity + repository only | No service/controller — nothing calls it yet |
| `knowledge` | `KnowledgeFragment` entity + repository, `FragmentType` enum | `embedding` column exists in the DB but is **not mapped** on the entity yet |
| `common.exception` | `ResourceNotFoundException`, `InvalidRequestException` | Thrown by services, turned into HTTP responses centrally |
| `common.web` | `GlobalExceptionHandler`, `ApiError` | `@RestControllerAdvice` — all four exception types funnel through one place |
| `ingestion`, `parser`, `embedding`, `retrieval`, `generation`, `prompt`, `validation`, `similarity`, `export`, `audit`, `config` | Empty except `package-info.java` | Each `package-info.java` says which milestone owns it and which spec section it implements — **read those before creating a new top-level package**, the slot is probably already reserved |

Schema: `resume-ai/backend/src/main/resources/db/migration/V1__init_schema.sql`
(single Flyway migration). Tables: `candidate`, `client`, `candidate_project`,
`technology`, `knowledge_fragment`. Field-by-field mapping to the spec is in
§8.1/§8.2/§8.3/§8.5/§8.7 — the migration follows those exactly except where
noted inline in the SQL comments.

## Deliberately deferred (do not re-derive these as "missing")

- **`project_technology` join table (§8.4)** — not created. Nothing about
  timeline-aware technology selection exists yet; that's Milestone 4.
- **`resume_source` table (§8.6)** — not created. `candidate_project.source_resume_id`
  and `knowledge_fragment.source_resume_id` are plain nullable `UUID` columns
  with no FK constraint, because the table they'd reference doesn't exist
  until Milestone 2 (resume ingestion). Add the FK when that table lands.
- **`knowledge_fragment.embedding`** — the column is in the migration
  (`vector(1536)`) so the table shape won't need a later migration just to
  add it, but it is **not mapped on the JPA entity**. Mapping a `vector`
  column needs either a custom Hibernate `UserType` or a vector-aware
  dialect helper (e.g. `hibernate-vector` or hand-rolled), and 1536 is a
  placeholder dimension (OpenAI `text-embedding-3-small` size) — confirm the
  real embedding model before wiring this up in Milestone 3.
- **Client name normalization is naive.** `ClientNameNormalizer` only trims,
  collapses whitespace, and upper-cases. The real alias table from §29
  ("J.P. Morgan" / "JPMC" → "JPMorgan Chase") does not exist. Right now
  "AT&T" and "A T & T" are two different clients. Don't be surprised by
  that — it's tracked, not a bug.
- **No overlapping-project or impossible-total-experience checks (§29).**
  Only `start_date <= end_date` is enforced (service-layer `InvalidRequestException`
  in `CandidateProjectService`, backstopped by a DB `CHECK` constraint in the
  migration). The richer chronology/consistency rules belong in the future
  `validation` package.
- **No frontend.** §56's tech stack list mentions React/TS/Vite, but Milestone 1's
  actual deliverable list (§43, §55) is backend-only. Frontend is Milestone 7.

## Design decisions worth knowing before you extend this

- **UUID primary keys everywhere**, generated by Postgres (`gen_random_uuid()`
  as the column default) rather than the application. Matches the `"uuid"`
  examples throughout the API design section (§21).
- **No Lombok.** Entities/DTOs are written out by hand (constructors +
  getters). This was a deliberate call to avoid an unverified annotation-processor
  config in an environment where the build couldn't be tested — plain
  Java is more likely to compile correctly on the first real build. Feel
  free to introduce Lombok later once the build is verified working, but
  do it as its own change, not mixed into a feature commit.
- **DTOs are Java records**, entities are not. Standard separation — never
  return JPA entities directly from a controller.
- **Bean Validation (`jakarta.validation`) on request DTOs**, business-rule
  validation (chronology, duplicate email, client upsert) in services.
  `GlobalExceptionHandler` distinguishes the two (`VALIDATION_FAILED` vs
  `BAD_REQUEST` in the `error` field of `ApiError`).
- **Client creation is an upsert, not a strict create.** Since clients are
  shared across candidates (§8.2 — "AT&T" will be referenced by dozens of
  candidate projects), `POST /api/v1/clients` returns the existing row if
  `normalized_name` already matches, instead of throwing a conflict. This
  wasn't explicitly specified — it's an interpretation call. Revisit if the
  frontend needs to distinguish "created" from "already existed" (currently
  both return `201`).
- **`spring.jpa.hibernate.ddl-auto: validate`** — Flyway is the only thing
  allowed to change the schema. Hibernate just checks the entity mappings
  agree with what Flyway created. If you add a field to an entity, you MUST
  also add a new Flyway migration (`V2__...sql`); editing `V1` after it's
  been applied anywhere is a Flyway checksum error waiting to happen.
- **Spring Boot 3.5.16 pinned in `pom.xml`.** The spec says "Spring Boot 3.x"
  (§6, §56); 3.5.16 is the last 3.x release before the project line moved to
  4.x. Confirm this is still what's wanted before a real build — Spring Boot
  4.x existing upstream wasn't accounted for when the spec was written.

## Commands to run locally

```bash
# 1. Start Postgres+pgvector only (for local `mvn spring-boot:run` against it)
cd resume-ai
docker compose up -d postgres

# 2. Run the backend against it
cd backend
DB_HOST=localhost mvn spring-boot:run

# --- or run everything in containers ---
cd resume-ai
docker compose up --build

# --- run tests (needs Docker for Testcontainers) ---
cd backend
mvn verify
```

Smoke-test once it's up:

```bash
curl -X POST localhost:8080/api/v1/candidates \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","primaryRole":"Java Full Stack Developer","totalExperienceYears":12}'

curl -X POST localhost:8080/api/v1/clients \
  -H 'Content-Type: application/json' \
  -d '{"name":"AT&T","industry":"Telecommunications"}'

# then POST /api/v1/candidates/{candidateId}/projects with the returned clientId
```

## API surface implemented so far

```
POST /api/v1/candidates                              -> 201 CandidateResponse
GET  /api/v1/candidates/{candidateId}                 -> 200 CandidateResponse | 404
POST /api/v1/clients                                  -> 201 ClientResponse (upsert by normalized name)
POST /api/v1/candidates/{candidateId}/projects         -> 201 ProjectResponse | 404 (bad candidate/client) | 400 (bad dates/validation)
GET  /api/v1/candidates/{candidateId}/projects         -> 200 ProjectResponse[]
```

The `GET /api/v1/candidates/{candidateId}/projects` list endpoint isn't in
§21's explicit list but was added because Milestone 1 already needs some way
to inspect what got created, and it's a natural sub-resource GET — flagged
here in case a future pass wants to reconcile it with the formal API design
section.

Everything else in §21 (`/resumes/upload`, `/resume-generations`, `/export`,
etc.) is intentionally not implemented — those belong to Milestones 2, 5, 6, 8.

## Suggested next step (Milestone 2, per §44's ordering)

§44 explicitly says build retrieval/generation against manually-entered data
*before* the resume parser, so the natural next milestone per that ordering
is actually **Milestone 3 (embeddings + retrieval)**, using the
`candidate` / `client` / `candidate_project` data this milestone lets you
create by hand via the API. Resume ingestion/parsing (Milestone 2 in §43's
numbering) can come after. Whoever picks this up should re-read §44's
rationale before deciding which to do first — it's a deliberate sequencing
choice in the spec, not an oversight.
