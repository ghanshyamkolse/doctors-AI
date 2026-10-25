# Doctors AI

AI-powered clinical scribe platform for doctors. The system accepts consultation audio or raw
transcripts, extracts a structured SOAP note and medication list, and stores the clinical
encounter for clinician review.

> **Status:** development scaffold. AI extraction and speech-to-text are deterministic mocks.
> This software is not a medical device and must not be used for clinical decisions without
> clinician review, validation, and the appropriate regulatory clearances.

## Repository Layout

```
doctors AI/
├── README.md                    # project overview (this file)
├── .vscode/                     # shared editor and launch configuration
└── ai-clinical-scribe/          # Spring Boot backend service
    ├── src/                     # application and test sources
    ├── pom.xml
    ├── docker-compose.yml
    ├── implementation_plan.md   # architecture, schema, API contract, roadmap
    └── README.md                # backend documentation
```

The repository currently contains the backend service. The `ai-clinical-scribe/` folder is a
standard Maven project; open or import that folder (the one containing `pom.xml`) in your IDE.

## Components

| Component | Description | Status |
| --- | --- | --- |
| `ai-clinical-scribe` | Spring Boot backend: patients, consultations, SOAP extraction, prescriptions, transcription | Development scaffold (mock AI) |

## Getting Started

Prerequisites:

- JDK 25 or newer (`java -version`)
- Maven 3.9+ or the included Maven Wrapper (`./mvnw` / `.\mvnw.cmd`)
- PostgreSQL 16 locally, or Docker for the provided compose file

Run everything from the backend folder:

```bash
cd ai-clinical-scribe
docker compose up -d
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd ai-clinical-scribe
docker compose up -d
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

## Documentation

- [Backend README](ai-clinical-scribe/README.md) — setup, configuration, API overview, testing, and
  troubleshooting.
- [Implementation plan](ai-clinical-scribe/implementation_plan.md) — architecture, database schema,
  API contract, and the roadmap for real Whisper/LLM integrations.

## Roadmap

1. Replace the mock LLM with a structured-output provider adapter.
2. Replace the mock transcription with Whisper or self-hosted `faster-whisper`.
3. Add database migrations, pagination, and containerized integration tests.
4. Add authentication, audit logging, PHI-safe logging, and a clinician review/sign-off workflow.
5. Harden for deployment: rate limits, retries, circuit breakers, tracing, and CI/CD.
