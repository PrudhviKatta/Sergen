# resume-ai

AI-assisted base resume generator for a consultancy. Full design spec:
[`docs/PHASE1_BASE_RESUME_GENERATOR.md`](docs/PHASE1_BASE_RESUME_GENERATOR.md).

Current status, decisions, and what's next for anyone (human or LLM) picking
this up: [`docs/IMPLEMENTATION_NOTES.md`](docs/IMPLEMENTATION_NOTES.md) —
**read that before writing code here.**

## Layout

```
backend/    Spring Boot 3 (Java 21) modular monolith
frontend/   not started yet (Milestone 7)
database/   Flyway migrations live under backend/; database/seed/ reserved for seed data
prompts/    reserved for versioned LLM prompt templates (Milestone 5, §33)
docs/       design spec + implementation notes
```

## Quick start

```bash
docker compose up -d postgres
cd backend && DB_HOST=localhost mvn spring-boot:run
```

See `docs/IMPLEMENTATION_NOTES.md` for the full command list, current API
surface, and test instructions.
