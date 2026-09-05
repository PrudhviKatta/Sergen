---
name: resume-ai-backend
description: Build, test, and run the Sergen resume-ai Spring Boot backend (resume-ai/backend/) without the user having to recall the exact Maven/Docker/JAVA_HOME incantation each time. Use this whenever the user asks to verify, test, build, compile, or run the backend/app/server for this repo, or asks things like "is the build passing", "run mvn verify", "start the app so I can curl it", "run just this one test", or mentions CandidateApiIT/KnowledgeFragmentApiIT/TechnologyTimelineValidatorTest or similar test class names from this codebase. Also trigger for "check the backend", "does it still compile", or "kick off the tests" in the context of this repo.
---

# Sergen resume-ai backend runner

This wraps the commands already documented in `CLAUDE.md` at the repo root so
they don't have to be reconstructed from memory each session. Read
`CLAUDE.md` first if anything here seems out of date with it — that file is
the source of truth for commands, this skill is just the "do it and report
clearly" wrapper.

There are two distinct things the user might want. Figure out which one from
their phrasing before running anything:

- **"verify" / "test" / "does it build"** → run the test suite. Go to
  [Verify](#verify).
- **"run it" / "start the app" / "let me curl it"** → run the actual server
  so it can be hit over HTTP. Go to [Run locally](#run-locally).
- **"run just this one test"** → a single test class or method. Go to
  [Single test](#single-test).

Both flows need Java 21 first — see [JAVA_HOME](#java_home-21).

## JAVA_HOME (21)

This project's `pom.xml` targets Java 21 specifically (`<java.version>21</java.version>`).
Whatever the ambient `java`/`JAVA_HOME` resolves to might not be 21 (this repo's
dev machine also has a newer generic `openjdk` installed as a Maven
dependency, which is a different, non-21 version). Before any `mvn` command:

```bash
java -version 2>&1 | grep -q "21\." || echo "not 21, need to locate a JDK 21"
```

If it's not already 21, find one rather than assuming a path:

```bash
# macOS with the system java wrapper:
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
# Homebrew keg-only fallback (this repo's dev machine installed it this way):
[ -z "$JAVA_HOME" ] && [ -d "$(brew --prefix openjdk@21 2>/dev/null)" ] && export JAVA_HOME="$(brew --prefix openjdk@21)"
```

If neither resolves anything, tell the user Java 21 isn't installed and offer
to install it (`brew install openjdk@21` on macOS) rather than guessing
further.

## Verify

Full test suite: Surefire (unit, `*Test.java`) + Failsafe (integration,
`*IT.java`, real Postgres+pgvector via **Testcontainers**). This does **not**
need `docker compose up` — Testcontainers starts its own throwaway Postgres
container automatically. It only needs the Docker *daemon* running:

```bash
docker info > /dev/null 2>&1 || echo "Docker daemon isn't running — start Docker/OrbStack first"
```

Then, from `resume-ai/backend/`:

```bash
mvn verify
```

**Do not report success from "BUILD SUCCESS" alone.** This codebase was
burned once already by a green build that silently ran zero integration
tests (a missing Failsafe plugin binding — see
`resume-ai/docs/IMPLEMENTATION_NOTES.md`, bug #1 in the Milestone 1 section).
Always pull the actual counts out of the output and report them:

```bash
mvn verify 2>&1 | grep -E "Tests run:|BUILD"
```

Report as: unit test count, integration test count, pass/fail, and the
`BUILD SUCCESS`/`BUILD FAILURE` line. If any "Tests run:" line reads `0` where
tests were expected, that's a red flag worth calling out explicitly, not a
pass — it's exactly the failure mode that bit this project once.

For "compile only, don't run tests":

```bash
mvn -DskipTests package
```

## Run locally

This needs a real, persistent Postgres — start it via Docker Compose (from
`resume-ai/`, not `resume-ai/backend/`):

```bash
cd resume-ai && docker compose up -d postgres
```

Wait for it to actually be ready before starting the app (starting the app
against a Postgres that's still initializing just produces a confusing
connection-refused loop):

```bash
until docker exec resume-ai-postgres-1 pg_isready -U resumeai -d resumeai > /dev/null 2>&1; do sleep 1; done
```

Then start the app in the background (it's a long-running process, don't
block on it) from `resume-ai/backend/`:

```bash
DB_HOST=localhost nohup mvn spring-boot:run > /tmp/resume-ai-app.log 2>&1 &
```

Poll the log rather than guessing a fixed sleep — startup time varies:

```bash
for i in $(seq 1 30); do
  grep -q "Started ResumeAiApplication" /tmp/resume-ai-app.log && break
  grep -qi "APPLICATION FAILED TO START\|Error starting" /tmp/resume-ai-app.log && break
  sleep 2
done
tail -20 /tmp/resume-ai-app.log
```

Once started, it's at `localhost:8080`. Tell the user it's up and give the
base URL — don't just say "done", since the actual usefulness is being able
to curl it.

`OPENAI_API_KEY` is only needed if the user wants to hit
`/api/v1/knowledge-fragments` or its search endpoint — everything else works
with no external config. The real key lives in a gitignored
`resume-ai/backend/.env` (never in this skill, never printed) — source it
into the same command that starts the app if those endpoints are needed:

```bash
cd resume-ai/backend && set -a && source .env && set +a && DB_HOST=localhost nohup mvn spring-boot:run > /tmp/resume-ai-app.log 2>&1 &
```

When done, stop it cleanly rather than leaving it orphaned:

```bash
pkill -f "spring-boot:run"
```

## Single test

Surefire (`*Test.java`, unit) and Failsafe (`*IT.java`, integration) use
different property names — using the wrong one silently runs nothing for
that class, the same trap as bug #1 above. Check the class name's suffix:

```bash
# unit test (ends in "Test"), runs under the `test` phase:
mvn -Dtest=VectorCodecTest test
mvn -Dtest=VectorCodecTest#roundTripsThroughEncodeAndDecode test

# integration test (ends in "IT"), needs `verify`, not `test`:
mvn verify -Dit.test=CandidateApiIT -DfailIfNoTests=false
mvn verify -Dit.test=CandidateApiIT#createAndFetchCandidate -DfailIfNoTests=false
```

`-DfailIfNoTests=false` on the integration-test commands avoids a spurious
failure from Surefire's unrelated unit tests also running as part of
`verify`'s normal lifecycle (they're unaffected by `-Dit.test`, this is
expected, not a bug).
