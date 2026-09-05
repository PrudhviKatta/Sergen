# Implementation Notes — Milestones 1, 3 &amp; 4

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

CI (`.github/workflows/ci.yml`) runs `mvn verify` on every push/PR to `main`
and `dev` on GitHub's own runners (Docker preinstalled, Testcontainers works
with zero extra config) — added specifically so bug #1 above (tests silently
not running) can't happen invisibly again.

## Status: Milestone 4 complete and build-verified (2026-09-03)

Built the §14 Technology Timeline Engine, per the deliverable list in §43
("Technology catalog, Timeline validation, Era profiles, Allowed technology
selection"). Deliberately built **before** Milestones 2/3 — see "Why Milestone
4 before 2/3" below. `mvn verify`: 20/20 tests green (7 unit + 13 integration).

What it does, concretely:
- `validation.TechnologyTimelineValidator.check(name, startYear, endYear)` —
  looks up a technology in the seeded catalog and returns one of
  `PASS` / `QUESTIONABLE` / `FAIL` / `UNKNOWN` with a human-readable reason.
  Three tiers, not binary — §14's own worked example distinguishes "safe use"
  from "prefer use" year thresholds for the same technology (Spring Boot:
  released 2014, "prefer 2015+"), which a strict pass/fail can't represent.
  `UNKNOWN` covers a technology name not in the catalog — deliberately not
  collapsed into PASS or FAIL, since neither would be honest.
- `validation.TechnologyTimelineValidator.suggestAlternatives(startYear, endYear)` —
  returns the union of every §40 era profile's technologies whose date range
  overlaps the given project range, alphabetized. A project spanning 2014-2017
  legitimately draws from both the 2012-2015 and 2016-2019 profiles.
- `technology.EraProfile` / `era_profile_technology` — new entity + join table
  (`V2__era_profiles_and_technology_seed.sql`), five rows transcribed directly
  from §40 (2008-2011 through 2024+), each linked to the technologies §40
  actually lists for it.
- The technology catalog itself: 42 rows, seeded in the same V2 migration,
  covering every technology named across §14/§40/§41. Years are **curated
  approximations**, not verified historical fact — e.g. Oracle and JavaScript
  are modeled as "always mainstream" (1979/1995) rather than precisely dated,
  since the engine only needs to catch clearly anachronistic combinations
  (Java 21 on a 2012 project), not pass a history exam. Maintained
  administratively per §14 — expect to hand-correct entries over time.
- `validation.ChronologyValidator` — the `start_date <= end_date` check that
  used to live inline in `CandidateProjectService` moved here, per the note
  the Milestone-1 `package-info.java` left for whoever built this package.

**Not built**: no REST endpoint. Nothing consumes this yet — the generation
pipeline that would call it (§12: "Retrieve Era-Appropriate Technologies" and
post-generation "Timeline Validation") doesn't exist until Milestone 5. Adding
a controller now would be API surface with no caller and no spec citation for
its shape — exactly what §56 says not to invent. When Milestone 5 wires the
generation pipeline, it calls this service directly (same JVM, same module),
not over HTTP.

**Two runtime roles, same module**: per §12, "Retrieve Era-Appropriate
Technologies" happens *before* generation (feeds `suggestAlternatives()`
results into the prompt), while "Timeline Validation" happens *after*
generation (feeds the LLM's chosen technologies through `check()`). Both are
implemented here now; the architecture diagram in
`docs/milestone-1-status.html` shows this module reading directly off the
technology catalog + era profiles, independent of the (still unbuilt)
retrieval engine.

### Why Milestone 4 before 2/3

§44's suggested build order is schema → manual data entry → embeddings →
retrieval → timeline engine → generation. This diverges from that on purpose:
the timeline engine has **zero dependency** on retrieval, embeddings, or an
LLM provider existing — it's pure date-math against a seeded table. Building
it now means Milestone 5 (generation) has one less unknown to wire up, and
the core "don't generate Kubernetes for a 2012 project" differentiator
(§3.3) is tested and working well before any LLM is involved. At the time
this was written, retrieval (Milestone 3) was still blocked on an embedding
provider decision (§26/§49) — that's since been resolved; see the Milestone 3
section below.

## What's implemented

Directory: `resume-ai/backend/src/main/java/com/company/resumeai/`

| Package | Contents | Notes |
|---|---|---|
| `candidate` | `Candidate` entity, repository, service, controller, DTOs | Full CRUD-lite: create + get by id |
| `client` | `Client` entity, repository, service, controller, DTOs, `ClientNameNormalizer` | Create is upsert-by-normalized-name (see below) |
| `project` | `CandidateProject` entity, repository, service, controller, DTOs | Create + list-by-candidate; owns the chronology check |
| `technology` | `Technology`, `EraProfile` entities + repositories | No REST controller — read internally by `validation.TechnologyTimelineValidator` |
| `knowledge` | `KnowledgeFragment` entity/repository/service/controller, `FragmentType` enum | `embedding` mapped via `@ColumnTransformer` (Milestone 3, see below); `POST`/`GET .../search` endpoints |
| `validation` | `ChronologyValidator`, `TechnologyTimelineValidator`, `TimelineStatus`, `TechnologyTimelineCheck` | Milestone 4's timeline engine lives here; see the Milestone 4 section below |
| `embedding` | `EmbeddingClient`, `OpenAiEmbeddingClient`, `VectorCodec`, `EmbeddingGenerationException` | Milestone 3; see the Milestone 3 section below |
| `retrieval` | `RetrievalService`, `RetrievalFilter` | Milestone 3; combines vector search + structured filters |
| `common.exception` | `ResourceNotFoundException`, `InvalidRequestException` | Thrown by services, turned into HTTP responses centrally |
| `common.web` | `GlobalExceptionHandler`, `ApiError` | `@RestControllerAdvice` — all four exception types funnel through one place |
| `ingestion`, `parser`, `generation`, `prompt`, `similarity`, `export`, `audit`, `config` | Empty except `package-info.java` | Each `package-info.java` says which milestone owns it and which spec section it implements — **read those before creating a new top-level package**, the slot is probably already reserved |

Schema: `resume-ai/backend/src/main/resources/db/migration/`. Two Flyway
migrations: `V1__init_schema.sql` (`candidate`, `client`, `candidate_project`,
`technology`, `knowledge_fragment`) and `V2__era_profiles_and_technology_seed.sql`
(`era_profile`, `era_profile_technology`, plus the seed data for both). Never
edit `V1` — it's already applied; schema changes are new `V3`, `V4`, ... files.
Field-by-field mapping to the spec is in §8.1/§8.2/§8.3/§8.5/§8.7 — the
migration follows those exactly except where noted inline in the SQL comments.

## Deliberately deferred (do not re-derive these as "missing")

- **`project_technology` join table (§8.4)** — not created. This is what will
  eventually link a specific `candidate_project` to the technologies it
  actually used (with `confidence` + `source`, per §8.4) — that's Milestone 5+,
  once generation exists to populate it. The technology **catalog** and **era
  profiles** are built (Milestone 4); what's missing is the per-project link.
- **`resume_source` table (§8.6)** — not created. `candidate_project.source_resume_id`
  and `knowledge_fragment.source_resume_id` are plain nullable `UUID` columns
  with no FK constraint, because the table they'd reference doesn't exist
  until Milestone 2 (resume ingestion). Add the FK when that table lands.
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

# Knowledge fragments need OPENAI_API_KEY set (they call embed() synchronously):
export OPENAI_API_KEY=sk-...
curl -X POST localhost:8080/api/v1/knowledge-fragments \
  -H 'Content-Type: application/json' \
  -d '{"fragmentType":"PROJECT_SUMMARY","content":"Modernized a core banking ledger system","domain":"Banking"}'

curl 'localhost:8080/api/v1/knowledge-fragments/search?query=banking+ledger+modernization&domain=Banking'
```

## API surface implemented so far

```
POST /api/v1/candidates                              -> 201 CandidateResponse
GET  /api/v1/candidates/{candidateId}                 -> 200 CandidateResponse | 404
POST /api/v1/clients                                  -> 201 ClientResponse (upsert by normalized name)
POST /api/v1/candidates/{candidateId}/projects         -> 201 ProjectResponse | 404 (bad candidate/client) | 400 (bad dates/validation)
GET  /api/v1/candidates/{candidateId}/projects         -> 200 ProjectResponse[]
POST /api/v1/knowledge-fragments                       -> 201 KnowledgeFragmentResponse (embeds content synchronously)
GET  /api/v1/knowledge-fragments/search?query=...      -> 200 KnowledgeFragmentResponse[] (+ domain/role/clientId/startYear/endYear/limit params)
```

Two of these (`GET .../projects`, both `/knowledge-fragments` routes) aren't
in §21's explicit list — each was added because its milestone needed *some*
way to inspect/populate data that automation (the parser, the generation
pipeline) doesn't produce yet. Flagged here in case a future pass wants to
reconcile them with the formal API design section.

Everything else in §21 (`/resumes/upload`, `/resume-generations`, `/export`,
etc.) is intentionally not implemented — those belong to Milestones 2, 5, 6, 8.

## Status: Milestone 3 complete and build-verified (2026-09-03)

Built §13 (hybrid retrieval) and the embedding half of §24/§26. Provider:
OpenAI `text-embedding-3-small` (1536 dims — matches the schema placeholder
exactly, no migration needed). `mvn verify`: 30/30 tests green (14 unit, 16
integration) — including a real round-trip through the pgvector column,
which is the part that couldn't be assumed correct without running it.

What it does:
- `embedding.EmbeddingClient` (interface) / `OpenAiEmbeddingClient` (impl) —
  calls OpenAI's REST embeddings endpoint directly via `java.net.http.HttpClient`
  (no SDK dependency; Jackson for JSON is already on the classpath). Fails at
  **call time**, not startup, if `OPENAI_API_KEY` isn't set — most of the app
  doesn't need one.
- `knowledge.KnowledgeFragment.embedding` is now mapped: a `String` field
  holding pgvector's text literal (`[0.12,0.34,...]`), with Hibernate's
  `@ColumnTransformer` casting it to/from the real `vector(1536)` column
  (`write = "?::vector"`, `read = "embedding::text"`). Deliberately **not**
  using a third-party pgvector-Hibernate library (pgvector-java,
  hypersistence-utils) — this is ~10 lines, has no unverified dependency, and
  was confirmed working against real Testcontainers Postgres before being
  trusted. See `embedding.VectorCodec` for the `float[]` ↔ text conversion.
- `retrieval.RetrievalService` — embeds a query string, then one native SQL
  query (`KnowledgeFragmentRepository.findSimilar`) applies §13 step 1
  (structured filters: domain, role, client, year-range overlap — each only
  when non-null) and step 2 (pgvector `<=>` cosine-distance ordering)
  together. §13 step 3 (diversity selection, avoid near-duplicate sources) is
  **not** implemented — deferred to Milestone 5's generation pipeline, the
  actual consumer of these results.
- `POST /api/v1/knowledge-fragments` + `GET /api/v1/knowledge-fragments/search` —
  not in §21's original API list. Added because retrieval needs *something*
  to search, and resume ingestion (Milestone 2) doesn't exist yet to
  populate it automatically — same "manual data entry before automation"
  logic §44 itself uses to justify building embeddings before the parser.
  Same interpretation-call pattern as the earlier `GET .../projects` list
  endpoint from Milestone 1.

**Testing note**: `OpenAiEmbeddingClient` itself is unit-tested (request
building, response parsing, missing-key handling — pure logic, no network
call). The integration test (`KnowledgeFragmentApiIT`) swaps in a fake
`EmbeddingClient` bean (`@TestConfiguration` + `@Primary`) that maps identical
text to identical deterministic vectors (`new Random(text.hashCode())`) —
proves the pgvector round-trip, cosine ordering, and metadata filters all
work, without needing a real API call in CI.

**Live-verified against the real OpenAI API on 2026-09-05**, manually, once a
key with billing enabled was available (the first attempt hit
`insufficient_quota` — the key was valid but the account had no credits; that
is a distinct failure mode from a bad/missing key and worth telling apart
when debugging this later). Three checks, in order of what they actually
prove:
1. Raw `curl` to `https://api.openai.com/v1/embeddings` — confirmed the
   response shape matches what `parseEmbeddingResponse` expects: 200 OK,
   `data[0].embedding` is a 1536-length float array, `model` echoes back
   `text-embedding-3-small`.
2. `POST /api/v1/knowledge-fragments` against the real running app (not a
   test) — `hasEmbedding: true`, and `SELECT vector_dims(embedding) ...`
   against the actual Postgres row confirmed a real 1536-dim vector landed
   in the `vector(1536)` column via the `@ColumnTransformer` mapping.
3. **The one that actually matters**: created Banking/Telecom/Healthcare
   fragments, then ran `GET .../search` with differently-flavored queries.
   A banking-flavored query ranked the Banking fragment first; a
   telecom-flavored query flipped the ranking to put Telecom first;
   filtering by `domain=Healthcare` correctly excluded the other two. This
   is the first proof that real OpenAI embeddings produce *useful* semantic
   rankings for this domain, not just that the plumbing doesn't throw.

No test in the repo exercises the live API (deliberately — no key in CI), so
this stays a manual check. If it's worth automating later, add
`@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")` to
a new test so it runs wherever a key happens to be present without requiring
one everywhere.

**Found and fixed one real bug while verifying this**: `KnowledgeFragmentApiIT`
originally reused the domain value `"Banking"` across two test methods. Since
`AbstractIntegrationTest` deliberately shares one DB across the *entire test
suite* (no per-test transaction rollback — that's what makes the singleton
Testcontainers pattern fast), a later test's filter assertion (`hasSize(1)`)
saw 2 rows, not 1, because an earlier test's leftover row matched the same
filter. Fixed by giving each test method's fixture data a unique domain
(`"Banking-" + UUID.randomUUID()`). **Lesson: with this shared-DB test
design, every test's fixture data must be unique across the whole suite, not
just within its own test method** — a hardcoded value that "only this test
uses" today can collide with a value some *other* test adds tomorrow.

## Suggested next step

With Milestones 1, 3, and 4 done, the natural next step is **Milestone 5
(generation engine)**: prompt builder + LLM abstraction (§26, same
provider-interface pattern as `EmbeddingClient`) that actually calls
`RetrievalService` and `TechnologyTimelineValidator` together to draft a
resume section. That's the first point where an LLM text-generation call
(not just embeddings) gets wired in — pick a chat-completions provider before
starting (§26/§49 apply here too). Resume ingestion/parsing (Milestone 2) can
still come after, per §44's own reasoning for building it last — there's now
a manual entry point (`POST /knowledge-fragments`) that makes that safe to
keep deferring.
