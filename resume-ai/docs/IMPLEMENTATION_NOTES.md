# Implementation Notes — Milestones 1, 2, 3, 4, 5, 6 &amp; 7

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

## Status: Milestone 5 complete and build-verified (2026-09-04)

Built the §12 generation pipeline end to end for a single resume-generation
request: prompt builder (§16), LLM provider abstraction (§26), project
generation, and candidate-summary generation. `mvn verify`: 43/43 tests green
(24 unit, 19 integration).

What it does:
- `generation.LlmClient` (interface) / `generation.OpenAiLlmClient` (impl) —
  calls OpenAI's chat completions endpoint directly via
  `java.net.http.HttpClient`, same pattern as `embedding.OpenAiEmbeddingClient`:
  no SDK dependency, fails at **call time** (not startup) if `OPENAI_API_KEY`
  isn't set. Default model `gpt-4o-mini`, overridable via `OPENAI_CHAT_MODEL`.
- `prompt.ProjectGenerationPromptBuilder` / `prompt.SummaryGenerationPromptBuilder`
  — §16's system-prompt guardrails (no copying, no technologies outside the
  approved list, no invented awards/metrics/certifications) plus a
  user-message builder. Each has a `VERSION` constant (§33) recorded on the
  row it produced. Deliberately plain Java text blocks rather than
  file/DB-backed templates — §33's suggestion, but with only two prompts and
  one version each, a template-loading mechanism has no payoff yet. Revisit
  if a v2 needs to coexist with v1.
- `generation.ResumeGenerationService` — the orchestrator. For each input
  project: validates chronology (`validation.ChronologyValidator`, same 400
  on bad input as `CandidateProjectService`), resolves the approved
  technology list (known technologies filtered through
  `TechnologyTimelineValidator.checkAll` if supplied, otherwise
  `suggestAlternatives` from the era profile — the Milestone 4 engine's
  first real consumer), retrieves reference fragments
  (`RetrievalService.retrieveSimilar`), builds the prompt, and parses the
  LLM's JSON response into description/responsibilities/environment. After
  every project drafts, one more LLM call produces the candidate summary
  from all project descriptions. The whole thing runs **synchronously** in
  one request — no queue/async infra exists yet, and nothing in §12 requires
  one for Phase 1.
- A generation either completes or fails, and both are stored, not thrown as
  a 500: `LlmGenerationException` from *either* LLM call is caught at the top
  of `generate()` and recorded as `status=FAILED` with `failureReason` on the
  `ResumeGeneration` row itself. `InvalidRequestException` (bad chronology) is
  **not** caught here — that's a client-input problem, so it propagates to
  `GlobalExceptionHandler` as a 400 the same way it does everywhere else in
  the app, and nothing gets persisted.
- `POST /api/v1/resume-generations` + `GET /api/v1/resume-generations/{id}` —
  §21's "Generate Base Resume" / "Get Generation". Regenerate/approve/export
  are **not** built — they belong to the rewrite loop (Milestone 6) and
  export (Milestone 8), neither of which exists yet.
- `GeneratedProject.responsibilities`/`environment` are stored as one
  delimited text column each (newline- and comma-joined), not a child table —
  they're only ever rendered back as a list for their parent project, never
  queried individually, so a join table would add mapping complexity with no
  query benefit.

**Testing note**: `OpenAiLlmClient` is unit-tested the same way as
`OpenAiEmbeddingClient` — request building, response parsing, missing-key
handling, no network call. `ResumeGenerationApiIT` swaps in both a fake
`EmbeddingClient` (same deterministic-vector trick as `KnowledgeFragmentApiIT`)
and a fake `LlmClient` (`@TestConfiguration` + `@Primary`, returns a canned
JSON project response or a canned summary sentence depending on which system
prompt it was handed) — proves the whole pipeline wiring (retrieval → timeline
engine → prompt → parse → persist → summary) without a real API key or cost.
No test exercises the live OpenAI chat completions API — same reasoning and
same `@EnabledIfEnvironmentVariable` opt-in path as Milestone 3's embeddings,
if that's ever worth automating.

**Found and fixed one real bug while verifying this**: `V3__resume_generation.sql`
declared `total_experience_years` as `SMALLINT`, but `ResumeGeneration`'s Java
field is `Integer` — Hibernate's schema validation (`ddl-auto: validate`)
caught the mismatch at startup (`wrong column type encountered ... found
int2, but expecting integer`) before any test even ran. Fixed by changing the
column to `INTEGER`. Same lesson as Milestone 1's Failsafe-binding bug: this
is exactly the class of error `ddl-auto: validate` exists to catch, and it
only catches it if the app (or a test that boots the Spring context) actually
starts — never assume an entity/migration pair matches without running it.

## Status: Milestone 6 complete and build-verified (2026-09-04)

Built §17/§18's similarity checks and §19's rewrite loop, wired into
`ResumeGenerationService`'s per-project drafting step. `mvn verify`: 54/54
tests green (33 unit, 21 integration).

What it does:
- `similarity.SimilarityValidator` — embeds the candidate draft and every
  reference text (same `EmbeddingClient` abstraction as everything else),
  takes the max cosine similarity across them, and classifies it into §17's
  three bands: `< 0.55` ACCEPTABLE, `0.55-0.70` REVIEW, `>= 0.70` REWRITE.
- `similarity.DuplicatePhraseDetector` — §18's "12+ consecutive words match
  an existing resume" rule, via word-shingle set intersection (candidate's
  12-word windows vs. each reference text's) rather than a general
  sequence-matching/LCS library — the rule itself is an exact-match rule, so
  an exact-match implementation is enough. A hit forces REWRITE regardless of
  the semantic score — the two checks are independent, either can flag.
- `ResumeGenerationService.generateProject` — now a loop, not a single call:
  generate, score with `SimilarityValidator` against (a) the retrieval
  reference snippets already fetched for the prompt and (b) the descriptions
  of sibling projects already drafted earlier in *this same request* (§18's
  "resumes from the same candidate", scoped to one request — see below), and
  regenerate on a REWRITE verdict up to `MAX_REWRITE_ATTEMPTS = 3` total
  tries (§19: "limit rewrites to avoid infinite loops"). A retry appends one
  extra sentence to the same user prompt telling the model the previous
  draft was too similar — same prompt version, not a new one; the rewrite
  loop reuses `ProjectGenerationPromptBuilder`, it doesn't need its own.
- `GeneratedProject` gained four columns recording the *final* attempt's
  outcome: `similarityScore`, `similarityVerdict`, `duplicatePhraseDetected`,
  `rewriteAttempts`. Only the final result is stored — per-attempt history
  (what attempt 1 vs. attempt 2 actually said) is not, which is enough for
  Milestone 6's own deliverables but would need its own audit table if §32's
  full "rewrite attempts" auditability is ever built out properly.

**Scope decision worth knowing**: §18 also lists "previously generated
resumes" and "resumes from the same client/candidate" as things to check
against — this only covers the *current* request's siblings, not resumes
from earlier, separate `/resume-generations` calls. `ResumeGeneration.candidateName`
is a free-text field, not a foreign key to `candidate.Candidate`, so "this
candidate's other resumes" isn't a query that can be asked yet. Revisit once
generation requests link to a persisted candidate identity.

**Testing note**: `SimilarityValidator`/`DuplicatePhraseDetector` are pure
unit tests (`SimilarityValidatorTest` hand-picks embedding vectors via a
one-line `EmbeddingClient` lambda so exact cosine similarity is under the
test's control — no hash-based fake needed there). The rewrite loop itself
is proven against a real Testcontainers Postgres in two new integration
tests: `ResumeGenerationRewriteIT` (a fake `LlmClient` reproduces a seeded
knowledge fragment verbatim on attempt 1, forcing REWRITE, then returns a
different draft on the retry — the fake tells attempts apart by checking
whether the REWRITE_HINT text is present in the user prompt, no
counters/mutable state needed) and `ResumeGenerationRewriteCapIT` (a fake
`LlmClient` that *never* resolves, proving the loop stops at
`MAX_REWRITE_ATTEMPTS` instead of looping forever).

**Found and fixed one real bug while writing these tests**: the fake
`EmbeddingClient` pattern shared across every `*IT` test since Milestone 3
(`new Random(text.hashCode())`, filling a 1536-float vector with
`random.nextFloat()`) produces vectors with every component in `[0,1)`, not
`[-1,1)`. For two *unrelated* texts, that systematically biases cosine
similarity toward **~0.75, not ~0** — close enough to random chance that it
had never been caught, because every test before this milestone only
asserted *relative* ranking ("identical text ranks first"), never an
*absolute* similarity value. Milestone 6's `SimilarityValidator` is the first
thing to apply an absolute threshold (0.55/0.70) to these fake vectors, and
it immediately turned any two "different" fake-embedded texts into a
false REWRITE. Fixed by changing the fake to `random.nextFloat() * 2f - 1f`
(symmetric range) everywhere it's used (`KnowledgeFragmentApiIT`,
`ResumeGenerationApiIT`, and both new rewrite tests) — identical-text-ranks-first
assertions are unaffected (a vector dotted with itself is always cosine 1.0
regardless of range), but unrelated texts now average to ~0 similarity like
a real embedding model would. **Lesson: a fake that only proves relative
ordering can still be quietly wrong in an absolute sense — it just takes an
absolute-threshold test to notice**, the same category of gap as Milestone
1's "BUILD SUCCESS with zero tests run" bug, just one layer deeper (the test
infrastructure itself, not the app).

## Status: Milestone 7 complete and live-verified (2026-09-04)

Built §31's Screen 2 (Generate Resume) and Screen 3 (Review), plus a stubbed
Screen 1 (Upload), against the existing `/resume-generations` API. React 19 +
TypeScript + Vite + MUI (§6's recommended stack) via `npm create vite@latest`
scaffolding, not hand-rolled — the backend so far has been hand-written
without a scaffolding tool, but there's no equivalent value in reinventing
Vite's own template.

What it does:
- `GenerateResumePage` — §31 Screen 2's fields (candidate name, primary role,
  total experience, repeatable project rows: client/dates/role, plus §11's
  other optional fields domain/known-technologies) posting straight to
  `ResumeGenerationRequest`'s shape. Navigates to the review page with the
  response's `id` on success.
- `ReviewPage` / `GeneratedProjectCard` — §31 Screen 3: summary, one card per
  project with description/responsibilities/environment, plus (not in the
  spec's original screen, but the whole point of Milestone 6) a
  similarity-verdict chip, a duplicate-phrase warning, and a rewrite-attempt
  count. Approve/Edit/Regenerate buttons are rendered **disabled** with a
  tooltip explaining why — `ResumeGenerationController` has no
  regenerate/approve endpoint (§21, still deferred), and a disabled button
  that's honest about not working yet is better than one that silently does
  nothing.
- `UploadPlaceholderPage` — §31 Screen 1 (upload/view-parsing/edit/confirm)
  needs resume parsing (Milestone 2, §21's `POST /resumes/upload`), which
  doesn't exist. This screen exists so the nav entry is present, but it's an
  honest "not built yet" stub, not a fake upload flow with nothing behind it.
  Screen 4 (Export) isn't built at all — that's Milestone 8, not 7.
- `vite.config.ts` proxies `/api` to `localhost:8080` in dev;
  `frontend/nginx.conf` does the same in the built Docker image
  (`frontend/Dockerfile`, added to `docker-compose.yml`) — the app only ever
  calls a relative `/api/v1/...` path, so neither the backend nor the
  frontend needs any CORS configuration.

**Found and fixed two real bugs by actually running the full stack** (Postgres
+ backend + `npm run dev`, hitting the real API through the Vite proxy) rather
than trusting the build/tests alone:
1. **`UnexpectedRollbackException` on any embedding/LLM failure.**
   `ResumeGenerationService.generate()` was `@Transactional`, and
   `RetrievalService.retrieveSimilar()` (a *different* bean) runs inside its
   own `@Transactional(readOnly = true)`. When `embed()` threw
   `EmbeddingGenerationException` (trivially reproducible: no
   `OPENAI_API_KEY` configured), Spring marked the *shared physical*
   transaction rollback-only the instant the exception crossed that inner
   proxy boundary — before `generate()`'s own `catch` block ever ran. The
   later `resumeGenerationRepository.save(generation)` then failed with
   `UnexpectedRollbackException`, which `GlobalExceptionHandler`'s catch-all
   turned into an opaque 500 instead of the intended `FAILED` generation.
   **No test had ever exercised this** — every fake `EmbeddingClient` across
   every `*IT` test was designed to never throw. Fixed by removing
   `@Transactional` from `generate()` entirely (see its javadoc for the full
   reasoning) — it calls slow external APIs, so it shouldn't hold a DB
   transaction open across them regardless of this specific bug; the only
   write (`save()`) manages its own transaction via Spring Data's repository
   proxy. Added `ResumeGenerationEmbeddingFailureIT` as the regression test.
2. **`GlobalExceptionHandler`'s catch-all never logged the exception it was
   handling.** The response body deliberately never leaks internals
   ("Unexpected server error"), which meant the *only* place the real cause
   could have been visible was a server-side log — and that log call didn't
   exist. Diagnosing bug #1 above required adding it first. Fixed
   permanently, not just for debugging: `log.error("Unhandled exception on
   {}", req.getRequestURI(), ex)`.

Both were only reachable by actually running the app without a configured
API key and hitting it over HTTP — `mvn verify`'s fakes never throw, so
neither bug could have been caught by the existing automated suite alone.
**Lesson, same theme as Milestones 1 and 6's bugs**: a test suite that's
entirely built on fakes that always succeed can't find the failure paths —
verifying "does it actually run" against something that can genuinely fail
(a real API call with no key, here) is a different and necessary check.

**Live-verified end to end** (2026-09-04): with the real `OPENAI_API_KEY`
sourced from `.env`, `POST /api/v1/resume-generations` through the exact
`localhost:5173` → Vite proxy → `localhost:8080` path the React app itself
uses returned a `COMPLETED` generation with a real GPT-4o-mini summary and
project draft (era-appropriate technologies only, `similarityVerdict:
ACCEPTABLE`, one attempt, no rewrite needed); `GET` round-tripped it back
through the same proxy. **Not** verified: the page in an actual browser
window — no screenshot/browser-automation tool was available in this
session, so the UI's rendering, styling, and interactive behavior are
unverified beyond `tsc`/`vite build` succeeding and the exact data shape the
components consume being proven correct against the live backend.

## Status: Milestone 2 complete and live-verified (2026-09-04)

Built §9's resume ingestion flow, out of spec order (after 3-7, not right
after Milestone 1) - deliberately: §44 itself argues for building the parser
last since "generation architecture can be tested using manually inserted
clean data first," and that's exactly what happened. Building it now was
driven by the user directly asking for it ("uploading resumes to have in
DB"), not the milestone list order. `mvn verify`: 70/70 tests green (44
unit, 26 integration).

**Refactor first**: moved `LlmClient`/`LlmRequest`/`LlmResponse`/
`LlmGenerationException`/`OpenAiLlmClient` out of `generation` into a new
`llm` package. They started in `generation` (Milestone 5, its only
consumer); `parser` needing the same abstraction for resume parsing is
exactly the "second consumer" signal that it isn't generation-specific
anymore. Not one of §22's originally listed packages - a deliberate,
documented deviation (see `llm/package-info.java`), same category as
`POST /knowledge-fragments` not being in §21's original list either.

What it does:
- `parser.ResumeTextExtractor` — PDF (Apache PDFBox), DOCX (Apache POI/
  `XWPFDocument`), plain text (no library). Deliberately not supporting
  legacy `.doc` (HWPF, a different/older binary format) - rare enough not to
  justify a second code path. A corrupt/unsupported file is a 400
  (`InvalidRequestException`), not a 500 - it's a property of that specific
  upload, not a server problem.
- `parser.ResumeParser` — §10's structured extraction, via `llm.LlmClient`
  and the new `prompt.ResumeParsingPromptBuilder`. Rule-based section
  parsing was rejected as impractical for Phase 1 (resume layouts vary too
  much); the system prompt is explicit that inventing anything not in the
  source text is wrong, not just undesirable - a fabricated project here
  would silently corrupt the knowledge base, not just one generated resume.
  Dates are kept as raw extracted strings ("2016", "2016-01", "Present"),
  not parsed into `LocalDate` - see `ParsedProject`'s javadoc.
- `ingestion.ResumeUploadService` — orchestrates §9's flow: extract, parse,
  persist `ResumeSource` either way (PARSED or FAILED - raw text is saved
  *before* parsing is attempted, so it survives a parse failure), then
  create knowledge fragments (one `PROJECT_SUMMARY` from responsibilities,
  one `TECH_STACK` from the technology list, per parsed project) via the
  existing `KnowledgeFragmentService` - each embedded and searchable
  immediately. **Deliberately not `@Transactional`** - identical reasoning
  to `generation.ResumeGenerationService.generate()`'s javadoc (Milestone
  5/6): both `resumeParser.parse()` and `knowledgeFragmentService.create()`
  call out to the LLM/embedding APIs from their own separately-transactional
  beans, so wrapping this method risks the same `UnexpectedRollbackException`
  class of bug already found once. `ResumeUploadParsingFailureIT` is the
  regression test for this specific path (a malformed LLM response during
  parsing) - see Milestone 5/6's own bug notes above for the general lesson.
- **Deliberately does NOT auto-create `candidate_project` rows.** §31 Screen
  1 lists "Edit parsed projects" / "Confirm technologies" as separate
  actions after upload - that reads as a human-gated confirmation step, not
  something upload should do automatically. Milestone 2's own deliverables
  list (Upload API, text extraction, parser, structured JSON, DB
  persistence) doesn't mention it either. Not built.
- `knowledge_fragment.source_resume_id` and `candidate_project.source_resume_id`
  (plain UUID columns since V1, annotated "FK added in Milestone 2 once
  resume_source exists") got their actual FK constraints added in V4, now
  that `resume_source` exists. `KnowledgeFragment` gained
  `applySourceResume(UUID)` (mirroring `applyEmbedding`) since the 9-arg
  constructor already had callers that shouldn't need to change.
- Frontend: `UploadResumePage` replaced the Milestone 7 stub - real file
  picker, calls `POST /resumes/upload`, renders the parsed candidate/projects
  or the failure reason. `api/client.ts` gained `apiPostMultipart` (no
  `Content-Type` header - the browser sets the multipart boundary itself;
  setting it manually produces a request the backend can't parse).

**Live-verified end to end** (2026-09-04): uploaded a real `.txt` resume
through the actual `localhost:5173` → Vite proxy → `localhost:8080` path
with the real `OPENAI_API_KEY`. Parsing correctly extracted exactly what was
in the text (client, role, years, technologies, responsibilities) with
nothing fabricated; `GET /knowledge-fragments/search` confirmed both
resulting fragments were created, embedded (`hasEmbedding: true`), linked
back via `sourceResumeId`, and ranked top for a matching query. **Not**
re-verified visually in an actual browser window this session either (same
caveat as Milestone 7) - the data path is proven, the rendering isn't.

## Suggested next step

With Milestones 1-7 all done, the core §12 pipeline plus its two feeder
paths (manual entry and now resume upload) are complete:
- **A real browser check** of the whole frontend (`ReviewPage`,
  `GeneratedProjectCard`, and now `UploadResumePage`) - layout, MUI theming,
  and interactive behavior still haven't been visually confirmed by an
  actual screenshot/browser-automation tool across two milestones now.
- **Milestone 8 (export)** — Markdown/DOCX/PDF, plus the export screen
  (§31 Screen 4).
- **Regenerate/approve endpoints** (§21) and **confirming a parsed upload
  into real `candidate_project` rows** (§31 Screen 1's "Edit parsed
  projects"/"Confirm technologies") — both are human-gated review actions
  the UI currently has no backend for; they're related (both are "turn a
  draft into a committed record") and could reasonably be designed together.
- Seeding ~10 real knowledge fragments *or* just uploading ~10 real resumes
  now that upload works, would make retrieval/similarity results in a manual
  check meaningfully realistic instead of near-empty.
- The tone/quality LLM-judge (§29) is still an open scope question,
  unrelated to ingestion or the frontend.
