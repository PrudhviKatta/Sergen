# PHASE1_BASE_RESUME_GENERATOR

## 1. Purpose

This document defines Phase 1 of an AI-powered resume generation system for a consultancy.

The Phase 1 objective is simple:

**Input**
- Candidate identity/reference
- Client or employer names
- Project/employment timelines
- Optional role/title
- Optional total years of experience

**Output**
- A professionally written base resume
- Unique project summaries and bullets
- Technology choices that are consistent with the stated timeline
- Content grounded in an internal resume/project knowledge base
- A resume that the candidate can later review and tailor manually for a specific job description

A job description is **not required** in Phase 1.

---

## 2. Core Product Idea

The system should learn patterns from resumes and project histories already available to the consultancy.

When a user supplies only client names and timelines, the system should:

1. Identify relevant historical project patterns from the internal knowledge base.
2. Retrieve examples similar to the client, industry, role, and time period.
3. Infer reasonable project themes and technology combinations.
4. Generate fresh project descriptions rather than copying existing resumes.
5. Validate whether the technologies make sense for the given dates.
6. Check the generated resume for excessive similarity against existing resumes.
7. Produce a clean base resume for candidate review.

The system should treat generated descriptions and inferred technology details as **draft suggestions**, not verified facts, unless they are already known for that candidate.

---

## 3. Design Principles

### 3.1 Grounded Generation

The LLM should not independently invent complete experience histories.

Generation should be grounded using:
- Existing resumes
- Existing project descriptions
- Candidate-specific known facts
- Timeline-aware technology rules
- Client/domain metadata

### 3.2 Generate, Do Not Copy

Retrieved resumes are references, not templates to duplicate.

The system should synthesize:
- Project type
- Responsibilities
- Architecture
- Technology combinations
- Business context

The final wording should be newly generated.

### 3.3 Timeline Accuracy

The platform should not generate technologies that are inconsistent with the stated project dates.

Example:

If a project is from 2012-2014, the system should not casually generate:
- Java 21
- Spring Boot 3
- Kubernetes
- React 19
- AWS Lambda

unless there is explicit evidence that such technologies were actually used.

### 3.4 Candidate Review Required

Because Phase 1 may infer project details from patterns, the generated resume should be considered a draft.

The candidate or recruiter should review:
- Responsibilities
- Technologies
- Project descriptions
- Titles
- Domain assumptions

before the resume is treated as final.

---

## 4. Phase 1 Scope

### Included

- Resume ingestion
- Resume parsing
- Project extraction
- Skill extraction
- Technology extraction
- Timeline extraction
- Domain/client tagging
- Embedding generation
- Vector search
- Client/timeline-based resume generation
- Timeline-aware technology filtering
- Similarity checking
- Duplicate phrase detection
- Resume versioning
- Markdown output
- DOCX/PDF export support
- Candidate/recruiter review workflow

### Not Included

- Job-description-based tailoring
- Automated job application
- Automatic submission to ATS systems
- LinkedIn automation
- Interview preparation
- Candidate scoring against jobs
- Recruiter CRM
- Full autonomous factual verification

These can be introduced later.

---

## 5. High-Level Architecture

```text
                   +----------------------+
                   | Existing Resume Files |
                   +-----------+----------+
                               |
                               v
                    +----------------------+
                    | Resume Ingestion API |
                    +-----------+----------+
                                |
                                v
                    +----------------------+
                    | Resume Parser        |
                    | Project Extractor    |
                    +-----------+----------+
                                |
               +----------------+----------------+
               |                                 |
               v                                 v
     +--------------------+            +----------------------+
     | PostgreSQL         |            | Embedding Service    |
     | Structured Records |            +----------+-----------+
     +---------+----------+                       |
               |                                  v
               |                         +--------------------+
               |                         | PGVector           |
               |                         | Semantic Index     |
               |                         +---------+----------+
               |                                   |
               +----------------+------------------+
                                |
                                v
                     +----------------------+
                     | Generation Request   |
                     | Client + Timeline    |
                     +----------+-----------+
                                |
                                v
                    +-----------------------+
                    | Retrieval Engine      |
                    +----------+------------+
                               |
                               v
                    +-----------------------+
                    | Timeline Validator    |
                    +----------+------------+
                               |
                               v
                    +-----------------------+
                    | Prompt Builder        |
                    +----------+------------+
                               |
                               v
                    +-----------------------+
                    | LLM Generation        |
                    +----------+------------+
                               |
                               v
                  +---------------------------+
                  | Similarity / Quality /    |
                  | Tone Validator            |
                  +------------+--------------+
                               |
                               v
                    +-----------------------+
                    | Resume Renderer       |
                    | MD / DOCX / PDF       |
                    +-----------------------+
```

---

## 6. Recommended Technology Stack

### Backend

Recommended:
- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security later if needed

Alternative:
- Python + FastAPI

For a Java-focused consultancy, Spring Boot is a strong fit.

### Database

Use:

- PostgreSQL
- PGVector extension

This avoids introducing a separate vector database during the MVP.

PostgreSQL stores:
- Candidates
- Clients
- Projects
- Skills
- Technology metadata
- Generated resumes
- Versions

PGVector stores:
- Resume embeddings
- Project embeddings
- Bullet embeddings
- Project summary embeddings

### Frontend

Recommended:
- React
- TypeScript
- Vite
- Material UI

For Phase 1, the UI can remain very small.

### Local Development

Use Docker Compose for:
- PostgreSQL
- PGVector
- Backend
- Frontend
- Optional local LLM runtime

---

## 7. Why PGVector Instead of a Separate Vector Database

Phase 1 does not require Pinecone, Weaviate, Milvus, or another dedicated vector database.

PGVector is enough because:

- The data is naturally relational.
- Candidate information needs SQL.
- Client history needs SQL.
- Timelines need SQL.
- Projects need relational joins.
- Embeddings can live beside the structured records.
- Deployment is simpler.
- MVP cost can remain very low.

A dedicated vector database can be considered later if search volume or data size becomes very large.

---

## 8. Important Data Model

### 8.1 Candidate

```text
candidate
---------
id
first_name
last_name
email
primary_role
total_experience_years
summary
created_at
updated_at
```

---

### 8.2 Client

```text
client
------
id
name
normalized_name
industry
description
created_at
```

Examples:

```text
JPMorgan Chase
AT&T
ADP
CVS Health
UnitedHealthcare
Walmart
Mastercard
```

---

### 8.3 Candidate Project

```text
candidate_project
-----------------
id
candidate_id
client_id
project_name
role_title
start_date
end_date
domain
project_summary
source_resume_id
confidence_score
created_at
updated_at
```

---

### 8.4 Project Technology

```text
project_technology
------------------
id
project_id
technology_id
confidence
source
```

`source` examples:

```text
RESUME_EXPLICIT
RECRUITER_CONFIRMED
CANDIDATE_CONFIRMED
MODEL_INFERRED
```

This distinction is important.

---

### 8.5 Technology Catalog

```text
technology
----------
id
name
category
first_available_year
mainstream_from_year
deprecated_from_year
notes
```

Example:

```text
Spring Boot
category = backend_framework
first_available_year = 2014
mainstream_from_year = 2015
```

Example:

```text
Kubernetes
category = container_orchestration
first_available_year = 2014
mainstream_from_year = 2017
```

This table powers timeline validation.

---

### 8.6 Resume Source

```text
resume_source
-------------
id
candidate_id
file_name
file_type
raw_text
parsed_json
ingestion_status
created_at
```

---

### 8.7 Knowledge Fragment

A knowledge fragment is a reusable semantic unit.

```text
knowledge_fragment
------------------
id
candidate_id nullable
client_id nullable
project_id nullable
fragment_type
content
domain
role
start_year
end_year
embedding
source_resume_id
created_at
```

Possible `fragment_type`:

```text
PROJECT_SUMMARY
RESPONSIBILITY
ARCHITECTURE
TECH_STACK
ACHIEVEMENT
DOMAIN_DESCRIPTION
```

---

## 9. Resume Ingestion Flow

When an existing resume is uploaded:

```text
Upload
  |
  v
Extract Text
  |
  v
Identify Sections
  |
  +--> Summary
  +--> Skills
  +--> Client/Employer History
  +--> Project Dates
  +--> Roles
  +--> Responsibilities
  +--> Technologies
  |
  v
Normalize Data
  |
  v
Create Knowledge Fragments
  |
  v
Generate Embeddings
  |
  v
Store in PostgreSQL + PGVector
```

---

## 10. Resume Parsing Output

A parsed resume could look like:

```json
{
  "candidate": {
    "primaryRole": "Java Full Stack Developer",
    "totalExperience": 11
  },
  "projects": [
    {
      "client": "AT&T",
      "role": "Java Developer",
      "startDate": "2015-01",
      "endDate": "2017-06",
      "domain": "Telecommunications",
      "technologies": [
        "Java 8",
        "Spring MVC",
        "REST",
        "Oracle",
        "Jenkins"
      ],
      "responsibilities": [
        "...",
        "..."
      ]
    }
  ]
}
```

The parser should preserve both:
- Raw extracted text
- Normalized structured data

---

## 11. Generation Input

Phase 1 generation should require only a small amount of input.

Example:

```json
{
  "candidateName": "Candidate A",
  "primaryRole": "Java Full Stack Developer",
  "totalExperienceYears": 12,
  "projects": [
    {
      "client": "Bank of America",
      "startDate": "2014-02",
      "endDate": "2016-07"
    },
    {
      "client": "AT&T",
      "startDate": "2016-08",
      "endDate": "2020-03"
    },
    {
      "client": "CVS Health",
      "startDate": "2020-04",
      "endDate": "2026-08"
    }
  ]
}
```

Optional fields:

```text
role
location
known technologies
domain
known project name
confirmed responsibilities
```

---

## 12. Resume Generation Pipeline

```text
Client + Timeline Input
        |
        v
Normalize Client Names
        |
        v
Resolve Domain
        |
        v
Retrieve Similar Projects
        |
        v
Retrieve Era-Appropriate Technologies
        |
        v
Build Project Context
        |
        v
Generate Project Draft
        |
        v
Timeline Validation
        |
        v
Similarity Validation
        |
        v
Rewrite if Required
        |
        v
Assemble Full Resume
        |
        v
Candidate Review
```

---

## 13. Retrieval Strategy

Do not retrieve simply by keyword.

Use hybrid retrieval.

### Step 1: Structured Filters

Filter by:

- Role
- Domain
- Client
- Approximate project year
- Experience level

Example:

```sql
WHERE role_category = 'JAVA_FULL_STACK'
AND domain = 'BANKING'
AND start_year BETWEEN 2012 AND 2017
```

### Step 2: Vector Search

Then perform semantic similarity search.

For example:

```text
"Java banking application modernization project during 2014-2016"
```

The vector database may retrieve project fragments describing:

- Online banking
- Payments
- Customer profile
- Account management
- Fraud detection
- Loan servicing
- Transaction processing

### Step 3: Diversity Selection

Do not retrieve ten nearly identical fragments.

Select examples from different sources.

For example:

```text
Candidate A Resume
Candidate B Resume
Candidate C Resume
Candidate D Resume
```

This reduces copying.

---

## 14. Technology Timeline Engine

This should be a separate module.

Example rules:

```text
Java 8
Released: 2014
Safe use: 2014+

Spring Boot
Initial release: 2014
Prefer use: 2015+

Docker
Initial release: 2013
Common enterprise use: 2015+

Kubernetes
Initial release: 2014
Prefer use: 2017+

React
Initial release: 2013
Enterprise adoption: 2015+

Java 17
Released: 2021

Java 21
Released: 2023
```

The exact dates should be stored in the technology catalog and maintained administratively.

### Validation Example

Input:

```text
Client: XYZ Bank
Timeline: 2011-2013
```

Generated technologies:

```text
Java 17
Spring Boot 3
Kafka Streams
Kubernetes
React
```

Validator result:

```text
FAIL
```

Possible alternatives:

```text
Java 6/7
Spring Framework
Spring MVC
Hibernate
JSP
JavaScript
JQuery
SOAP
REST
Oracle
WebLogic
Maven
Jenkins/Hudson
```

---

## 15. Client-Aware Project Generation

Client names can provide useful context but should not be treated as proof of a specific project.

For example:

```text
Client: AT&T
```

Possible domain signals:

```text
Telecommunications
Network analytics
Customer care
Billing
Order management
Provisioning
Network operations
Digital channels
```

The retrieval engine can use internal AT&T project examples and telecom examples.

However:

The LLM should not state that the candidate built a specific AT&T system unless that detail is known or explicitly marked as a generated suggestion.

---

## 16. Prompt Builder

The LLM should receive a controlled prompt generated by the backend.

Example concept:

```text
SYSTEM:
You generate professional software engineering resume content.

Use the supplied internal examples only as contextual references.

Do not copy sentences.

Do not claim technologies outside their valid timeline.

Do not invent awards, metrics, team sizes, certifications, or business outcomes.

When information is inferred rather than confirmed, prefer generic but realistic wording.

Generate varied, original wording.

INPUT PROJECT:
Client: AT&T
Timeline: 2016-2019
Role: Java Full Stack Developer

DOMAIN CONTEXT:
Telecommunications

APPROVED TECHNOLOGIES:
Java 8
Spring Boot
Spring MVC
REST
Oracle
Angular
Jenkins
Docker
AWS

REFERENCE PROJECT PATTERNS:
...
```

---

## 17. Resume Uniqueness Strategy

The requirement should not be:

> No two resumes can share any wording.

That would damage quality.

Instead, define uniqueness as:

- No large copied blocks
- No identical project summaries
- No repeated bullet sequences
- Different sentence structures
- Different ordering of responsibilities
- Different but realistic project emphasis
- Candidate-specific chronology

### Similarity Levels

Example:

```text
0.00 - 0.55 = acceptable
0.55 - 0.70 = review
0.70+       = rewrite
```

The actual threshold should be tuned using internal data.

---

## 18. Similarity Checker

Perform similarity checks against:

- Existing project summaries
- Existing bullet groups
- Previously generated resumes
- Resumes from the same client
- Resumes from the same candidate

Two checks are recommended.

### Semantic Similarity

Embedding comparison.

### Text Similarity

Use:
- N-gram overlap
- Sequence matching
- Longest common subsequence
- Duplicate phrase detection

Example rule:

```text
If 12+ consecutive words match an existing resume,
flag the bullet.
```

---

## 19. Rewrite Loop

```text
Generate
   |
   v
Similarity Score + Quality Score (§29 Tone & Quality Validation)
   |
   +---- Both Pass ----> Accept
   |
   +---- Either Fail --> Rewrite
                          |
                          v
                    Similarity Score + Quality Score
                          |
                          v
                        Accept
```

Limit rewrites to avoid infinite loops.

Example:

```text
MAX_REWRITE_ATTEMPTS = 3
```

---

## 20. Resume Structure

A standard generated base resume may contain:

```text
Name
Contact Information

Professional Summary

Technical Skills

Professional Experience

Client A
Role
Timeline
Project Description
Responsibilities
Environment

Client B
Role
Timeline
Project Description
Responsibilities
Environment

Education
Certifications
```

The actual template can remain configurable.

---

## 21. API Design

### Upload Resume

```http
POST /api/v1/resumes/upload
```

Request:

```text
multipart/form-data
```

Response:

```json
{
  "resumeId": "uuid",
  "status": "PROCESSING"
}
```

---

### Get Parsed Resume

```http
GET /api/v1/resumes/{resumeId}
```

---

### Generate Base Resume

```http
POST /api/v1/resume-generations
```

Request:

```json
{
  "candidateName": "Candidate A",
  "primaryRole": "Java Full Stack Developer",
  "totalExperienceYears": 12,
  "projects": [
    {
      "client": "AT&T",
      "startDate": "2016-01",
      "endDate": "2020-01"
    }
  ]
}
```

---

### Get Generation

```http
GET /api/v1/resume-generations/{generationId}
```

---

### Regenerate Project

```http
POST /api/v1/resume-generations/{generationId}/projects/{projectId}/regenerate
```

---

### Accept Project

```http
POST /api/v1/resume-generations/{generationId}/projects/{projectId}/approve
```

---

### Export

```http
GET /api/v1/resume-generations/{generationId}/export?format=docx
```

Supported:

```text
markdown
docx
pdf
```

---

## 22. Backend Modules

Recommended Spring Boot modules/packages:

```text
com.company.resumeai

config
candidate
client
resume
ingestion
parser
project
knowledge
embedding
retrieval
technology
generation
prompt
validation
similarity
export
audit
common
```

---

## 23. Suggested Project Structure

```text
resume-ai/
│
├── backend/
│   ├── src/main/java/
│   │   └── com/company/resumeai/
│   ├── src/main/resources/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   └── package.json
│
├── database/
│   ├── migrations/
│   └── seed/
│
├── prompts/
│   ├── project-generation.md
│   ├── summary-generation.md
│   └── rewrite.md
│
├── docs/
│   └── PHASE1_BASE_RESUME_GENERATOR.md
│
├── docker-compose.yml
└── README.md
```

---

## 24. Embedding Strategy

Create embeddings for:

```text
Project summary
Individual responsibility
Grouped responsibilities
Technology/environment section
Candidate summary
```

Do not create only one embedding for the entire resume.

Smaller semantic chunks provide better retrieval.

Recommended chunk size:

```text
Approximately 100-400 words
```

depending on content type.

---

## 25. Retrieval Metadata

Each embedding should include searchable metadata:

```json
{
  "client": "AT&T",
  "domain": "Telecommunications",
  "role": "Java Full Stack Developer",
  "startYear": 2016,
  "endYear": 2019,
  "technologyCategories": [
    "JAVA",
    "SPRING",
    "FRONTEND",
    "DATABASE",
    "DEVOPS"
  ]
}
```

This enables filtered vector search.

---

## 26. LLM Options

Phase 1 should keep the LLM provider abstract.

Create an interface:

```java
public interface LlmClient {
    LlmResponse generate(LlmRequest request);
}
```

Implementations could later support:

```text
OpenAI
Azure OpenAI
Anthropic
Gemini
Local Ollama
Local vLLM
```

This prevents vendor lock-in.

---

## 27. Near-Zero-Cost Development Option

A mostly free local development stack can be:

```text
Spring Boot
React
PostgreSQL
PGVector
Docker
Ollama
Open-weight embedding model
Open-weight LLM
```

Possible local usage:

```text
Embeddings -> local model
Generation -> local LLM
Database -> local PostgreSQL
```

Quality may be lower than a strong hosted LLM, but it is enough for architecture development and testing.

Production can later use paid inference.

---

## 28. Recommended LLM Separation

Do not use one giant prompt for everything.

Create separate generation tasks:

```text
1. Candidate Summary Generator
2. Project Context Generator
3. Responsibility Generator
4. Technology Environment Generator
5. Tone/Quality Judge (§29 Tone & Quality Validation)
6. Similarity Rewrite Generator
7. Final Resume Assembler
```

This improves control and debugging.

These do not need to be autonomous agents.

Simple services or prompt pipelines are sufficient.

---

## 29. Validation Rules

Before a generated resume is accepted:

### Timeline Validation

Check technology compatibility.

### Chronology Validation

Verify:

```text
start_date <= end_date
```

Check:
- Overlapping projects
- Impossible total experience
- Invalid date ranges

### Client Validation

Verify known client normalization.

Examples:

```text
J.P. Morgan -> JPMorgan Chase
JP Morgan -> JPMorgan Chase
JPMC -> JPMorgan Chase
```

### Technology Validation

Reject:
- Future technology
- Unsupported combinations
- Obvious version/date conflicts

### Duplication Validation

Compare against internal resumes.

### Content Safety Validation

Block fabrication of:
- Degrees
- Certifications
- Security clearances
- Awards
- Patents
- Quantified savings
- Team sizes
- Regulatory approvals

unless supplied or confirmed.

### Tone & Quality Validation

Human review (§31) catches naturalness issues but should not be the only gate — it does not scale and is inconsistent reviewer to reviewer.

Add an automated **Tone/Quality Validator** as its own generation task (consistent with §28's separation principle), run in the same pass as the Similarity Validator, before content reaches the reviewer.

Checks:

```text
Grammar / spelling correctness
Natural phrasing (not keyword-stuffed or robotic)
Professional tone consistency across bullets
Readability (sentence length/complexity variance)
No repeated sentence structure across bullets in one project
No repeated sentence structure across projects in one resume
Bullet-to-bullet redundancy (same idea restated)
```

Implementation: an LLM-as-judge call, separate prompt from the generator, scoring each project section independently. Do not let the generator grade its own output in the same call — use a distinct prompt/model call so the judge is not biased toward accepting its own phrasing.

Example score shape:

```json
{
  "grammar": 0.95,
  "naturalness": 0.72,
  "toneConsistency": 0.88,
  "structuralVariety": 0.65,
  "overall": 0.78
}
```

### Quality Thresholds

```text
0.85+      = accept
0.65-0.85  = rewrite
<0.65      = rewrite + flag for manual review after max attempts
```

Tune thresholds using the same golden-test approach as §46.

---

## 30. Confidence Model

Generated facts should carry confidence.

Example:

```text
CONFIRMED = 1.0
STRONGLY_INFERRED = 0.8
INFERRED = 0.6
WEAKLY_INFERRED = 0.4
```

UI can show:

```text
Confirmed
Suggested
Needs Review
```

This helps recruiters understand where AI made assumptions.

---

## 31. Recruiter/Candidate UI

Phase 1 UI can be very simple.

### Screen 1: Knowledge Base

Functions:

```text
Upload resume
View parsing result
Edit parsed projects
Confirm technologies
```

### Screen 2: Generate Resume

Fields:

```text
Candidate Name
Primary Role
Total Experience

Project 1
Client
Start Date
End Date
Role optional

Add Project
```

Button:

```text
Generate Base Resume
```

### Screen 3: Review

Show:

```text
Professional Summary

Project 1
[Approve]
[Edit]
[Regenerate]

Project 2
[Approve]
[Edit]
[Regenerate]
```

### Screen 4: Export

```text
Download DOCX
Download PDF
Download Markdown
```

---

## 32. Auditability

Every generation should store:

```text
Generation request
Retrieved fragment IDs
Prompt version
Model name
Generated output
Validation scores
Similarity score
Rewrite attempts
User edits
Approval status
Timestamp
```

This becomes important when diagnosing bad output.

---

## 33. Prompt Versioning

Prompts should not be hardcoded inside Java services.

Store prompts as files or database templates.

Example:

```text
prompts/
  project-generation-v1.md
  project-generation-v2.md
  summary-generation-v1.md
```

Each generated resume should record the prompt version used.

---

## 34. Feedback Loop

Recruiter edits are valuable training data.

Store:

```text
AI generated text
Human edited text
Accepted/rejected
Reason for rejection
```

Later, these pairs can improve:
- Prompting
- Retrieval
- Ranking
- Fine-tuning

Phase 1 does **not** need fine-tuning.

---

## 35. Do We Need Model Fine-Tuning?

No.

Not initially.

Start with:

```text
RAG + structured data + strong prompts + validation
```

Fine-tuning may be considered later after collecting enough examples of:

```text
Generated version
Human corrected version
```

For example:

```text
5,000-20,000 high-quality edit pairs
```

could eventually become useful training data.

---

## 36. Why RAG Is More Important Than Fine-Tuning Initially

Fine-tuning teaches writing behavior.

RAG supplies current company-specific knowledge.

Your main Phase 1 problem is:

```text
"What kinds of projects and technologies should be used for this client's timeline?"
```

That is primarily a retrieval and validation problem.

Therefore:

```text
RAG first
Fine-tuning later
```

---

## 37. Initial Knowledge Base Strategy

Start by ingesting high-quality resumes only.

Do not immediately load every resume available.

Use:

```text
20-50 strong resumes
```

covering different:

```text
Roles
Industries
Years
Technology stacks
Clients
```

Examples:

```text
Java Full Stack
AEM
Data Engineering
DevOps
Cloud
.NET
QA Automation
```

Then expand.

---

## 38. Data Cleaning Is Critical

Poor resumes will produce poor retrieval.

Before accepting knowledge:

```text
Remove duplicate bullets
Normalize technology names
Normalize clients
Correct timeline problems
Separate projects correctly
Remove placeholder text
Remove fake metrics
```

The internal knowledge base should be curated.

---

## 39. Project Archetypes

A powerful addition is a project archetype library.

Examples:

```text
BANKING_PAYMENT_PLATFORM
BANKING_ACCOUNT_PORTAL
HEALTHCARE_CLAIMS_PROCESSING
TELECOM_NETWORK_ANALYTICS
RETAIL_ECOMMERCE
INSURANCE_POLICY_PLATFORM
PAYROLL_PROCESSING
DATA_PIPELINE
CLOUD_MIGRATION
OBSERVABILITY_PLATFORM
```

Each archetype contains:

```text
Typical business capabilities
Common architecture
Technology categories
Era-specific technology variations
Common engineering responsibilities
```

This allows the system to create coherent projects even with minimal user input.

---

## 40. Era Profiles

Create technology-era profiles.

Example:

### 2008-2011

```text
Java 5/6
Spring Framework
Struts
JSP
Servlets
Hibernate
SOAP
Oracle
WebLogic
Ant/Maven
SVN
```

### 2012-2015

```text
Java 7/8
Spring MVC
Hibernate
REST
AngularJS
JQuery
Oracle
Maven
Jenkins
AWS emerging
```

### 2016-2019

```text
Java 8/11
Spring Boot
Microservices
REST
Angular/React
Kafka
Docker
AWS
Jenkins
Kubernetes emerging
```

### 2020-2023

```text
Java 11/17
Spring Boot
Microservices
Kafka
React
Angular
Docker
Kubernetes
AWS/Azure/GCP
Terraform
OpenTelemetry emerging
```

### 2024+

```text
Java 17/21
Spring Boot 3
Kubernetes
Kafka
Cloud-native
Observability
OpenTelemetry
AI-assisted development
Modern React
Platform engineering
```

These should be configuration, not hardcoded prose.

---

## 41. Example Phase 1 Generation

Input:

```text
Candidate:
Java Full Stack Developer
11 years

Client:
Regional Bank
2013-2016

Client:
Telecom Company
2016-2020

Client:
Healthcare Company
2020-2026
```

System reasoning pipeline:

```text
2013-2016
-> banking archetypes
-> Java 7/8 era
-> Spring MVC
-> Hibernate
-> REST/SOAP
-> AngularJS/JQuery
-> Oracle

2016-2020
-> telecom archetypes
-> Java 8
-> Spring Boot
-> Microservices
-> Kafka
-> Angular/React
-> AWS
-> Docker

2020-2026
-> healthcare
-> Java 11/17/21 depending exact dates
-> Spring Boot
-> Kafka
-> Kubernetes
-> cloud
-> React
-> observability
```

The LLM then converts that structured context into a cohesive resume.

---

## 42. Example Generated Project Shape

```text
Client: Telecom Company
Role: Java Full Stack Developer
Timeline: Aug 2016 - Mar 2020

Project:
Worked on a telecom operations platform supporting network monitoring,
service workflows, and operational analytics for internal engineering teams.

Responsibilities:
- Developed backend services using Java and Spring Boot for network and
  service-management workflows.
- Designed REST APIs used by web applications and downstream services.
- Built asynchronous processing flows using Kafka for high-volume operational events.
- Implemented reusable UI components for internal dashboards.
- Integrated application services with Oracle-backed operational datasets.
- Created CI/CD pipelines and automated deployment workflows.
- Containerized application components to improve environment consistency.
```

This is only an example pattern.

The production system should generate different wording and details based on retrieved context.

---

## 43. MVP Milestones

### Milestone 1

Database + schema

Deliverables:

```text
PostgreSQL
PGVector
Flyway migrations
Candidate schema
Client schema
Project schema
Knowledge fragment schema
Technology catalog
```

### Milestone 2

Resume ingestion

Deliverables:

```text
Upload API
Text extraction
Resume parser
Structured JSON
Database persistence
```

### Milestone 3

Embeddings + retrieval

Deliverables:

```text
Embedding provider
Vector storage
Vector search
Metadata filtering
```

### Milestone 4

Technology timeline engine

Deliverables:

```text
Technology catalog
Timeline validation
Era profiles
Allowed technology selection
```

### Milestone 5

Generation engine

Deliverables:

```text
Generation API
Prompt builder
LLM abstraction
Project generation
Summary generation
```

### Milestone 6

Similarity validator

Deliverables:

```text
Embedding similarity
Duplicate phrase check
Rewrite loop
```

### Milestone 7

Frontend

Deliverables:

```text
Resume upload screen
Generation input screen
Resume review screen
```

### Milestone 8

Export

Deliverables:

```text
Markdown
DOCX
PDF
```

---

## 44. Suggested MVP Order

Build in this exact order:

```text
1. PostgreSQL + PGVector
2. Candidate/project schema
3. Manual project data entry
4. Embeddings
5. Retrieval
6. Technology timeline engine
7. LLM generation
8. Similarity validation
9. Resume assembly
10. UI
11. Resume parser
12. Export
```

Why put the parser later?

Because generation architecture can be tested using manually inserted clean data first.

Resume parsing can become a separate complexity.

---

## 45. Minimum Viable Demo

The first demo should not try to solve everything.

Demo scenario:

```text
Knowledge Base:
30 curated Java resumes

User Input:
Candidate name
Java Full Stack Developer
3 client names
3 timelines

Click:
Generate

Output:
Professional Summary
Technical Skills
3 project sections
Timeline-aware environment
Similarity score
DOCX export
```

If this works well, Phase 1 is validated.

---

## 46. Testing Strategy

### Unit Tests

Test:

```text
Timeline validation
Client normalization
Technology selection
Similarity thresholds
Date calculations
```

### Integration Tests

Test:

```text
PostgreSQL
PGVector search
LLM provider
Generation workflow
```

### Golden Resume Tests

Maintain test examples.

Example:

```text
Input:
Bank
2012-2015

Expected:
No Java 17
No Spring Boot 3
No Kubernetes
No React 19
```

Another:

```text
Input:
Healthcare
2022-2026

Expected:
Modern Java acceptable
Spring Boot acceptable
Kubernetes acceptable
Cloud acceptable
```

---

## 47. Evaluation Metrics

Measure:

### Timeline Accuracy

```text
% of generated technologies valid for timeline
```

Target:

```text
> 98%
```

### Duplication

```text
% generated bullets above similarity threshold
```

Target:

```text
< 5%
```

### Human Acceptance

```text
% generated bullets accepted without major edits
```

Initial target:

```text
60-70%
```

Later target:

```text
80%+
```

### Generation Time

Target:

```text
< 30 seconds per resume
```

depending on model and number of projects.

---

## 48. Security Considerations

Resumes contain personal information.

Minimum security requirements:

```text
Encrypted transport
Authenticated access
Role-based authorization
Audit logs
Secure secrets
Encrypted backups
No production resumes in developer logs
```

Avoid sending unnecessary candidate PII to the LLM.

The generation request usually needs:

```text
Role
Experience
Client
Timeline
Project context
Technologies
```

It generally does not need:

```text
Phone
Home address
Immigration documents
SSN
Date of birth
```

---

## 49. LLM Data Privacy

Before choosing a hosted LLM provider, confirm:

```text
Data retention policy
Training policy
Enterprise privacy controls
Regional processing
API logging policy
```

Implement a provider abstraction so the organization can switch providers if required.

---

## 50. Future Phase 2

Phase 2 can introduce job-description tailoring.

Input:

```text
Base Resume
+
Job Description
```

Then:

```text
JD Parser
Skill Match
Project Selection
Bullet Re-ranking
Keyword Coverage
ATS-oriented Tailoring
Candidate Validation
```

This should remain separate from Phase 1.

---

## 51. Future Phase 3

Potential capabilities:

```text
Recruiter portal
Candidate portal
Job matching
Resume scoring
Reusable project library
Version comparison
Candidate approval workflow
Recruiter analytics
Fine-tuned models
Automated feedback learning
```

---

## 52. Recommended Final Architecture

For Phase 1:

```text
React
   |
Spring Boot
   |
   +---- PostgreSQL
   |
   +---- PGVector
   |
   +---- Embedding Provider
   |
   +---- Retrieval Engine
   |
   +---- Timeline Engine
   |
   +---- LLM Provider
   |
   +---- Similarity Validator
   |
   +---- DOCX/PDF Export
```

Do not over-engineer it with multiple microservices initially.

Build a modular monolith.

Each concern should be separated internally, but everything can initially run inside one Spring Boot backend.

---

## 53. Key Recommendation

The system should **not** be designed as:

```text
Client + Timeline
        |
        v
LLM
        |
        v
Resume
```

That approach will hallucinate.

The recommended design is:

```text
Client + Timeline
        |
        v
Structured Knowledge Retrieval
        |
        v
Timeline-Aware Technology Selection
        |
        v
Project Archetype Selection
        |
        v
LLM Generation
        |
        v
Similarity Validation
        |
        v
Candidate Review
        |
        v
Base Resume
```

That architecture gives the consultancy control over:

```text
Quality
Uniqueness
Technology accuracy
Consistency
Privacy
Cost
```

---

## 54. Phase 1 Success Definition

Phase 1 is successful when a recruiter can:

1. Select or enter a candidate.
2. Enter client names.
3. Enter project timelines.
4. Optionally enter role titles or known technologies.
5. Click **Generate Resume**.
6. Receive a complete professional base resume.
7. See technologies appropriate to each timeline.
8. See project content grounded in the internal knowledge base.
9. Receive minimal duplicate language from existing resumes.
10. Edit/regenerate individual projects.
11. Export the final draft.

No job description is required.

---

## 55. Immediate Development Starting Point

Start with the following implementation task:

```text
Create a Spring Boot application with PostgreSQL + PGVector.

Implement:

Candidate
Client
CandidateProject
Technology
KnowledgeFragment

Create Flyway migrations.

Create REST APIs to:

POST /candidates
POST /clients
POST /candidates/{candidateId}/projects
GET /candidates/{candidateId}

Add Docker Compose for PostgreSQL with PGVector.

Do not integrate an LLM yet.
```

Once this foundation is working, implement embeddings and retrieval.

---

## 56. Codex / Coding-Agent Starting Prompt

The following can be given to a coding agent as the initial implementation instruction:

```text
Read PHASE1_BASE_RESUME_GENERATOR.md completely before writing code.

Build Phase 1 as a modular monolith.

Backend:
Java 21
Spring Boot 3
Maven
PostgreSQL
PGVector
Flyway
Spring Data JPA

Frontend:
React
TypeScript
Vite

Start only with Milestone 1.

Create the project skeleton, Docker Compose, database schema,
JPA entities, repositories, services, controllers, validation,
global exception handling, and integration tests.

Do not implement LLM integration yet.

Do not invent requirements not contained in the design document.

Keep modules separated so embedding, retrieval, LLM generation,
similarity validation, and export can be added later.

After Milestone 1 is complete, provide:
1. files created
2. architecture explanation
3. commands to run locally
4. tests executed
5. remaining work
```

---

## 57. Final Note

The strongest Phase 1 approach is not to train a custom AI model from scratch.

Use:

```text
Curated resume knowledge
+
Structured candidate/project data
+
PGVector retrieval
+
Timeline intelligence
+
A strong existing LLM
+
Validation
+
Human review
```

This provides the fastest path to a useful internal product while keeping the architecture flexible enough for future resume tailoring, job matching, and recruiter automation.
