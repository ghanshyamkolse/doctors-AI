# AI Clinical Scribe — Implementation Plan

## 1. Purpose

`ai-clinical-scribe` is a Spring Boot 3.x backend for an AI-powered clinical scribe and voice
assistant. A clinician records a consultation (audio or text), the backend transcribes the audio,
extracts a structured SOAP note and medication list, and persists the encounter for review and
downstream use.

This document records the target architecture, the database schema, the API contract, the current
mock/stub boundaries, and the roadmap for replacing mocks with production Whisper and LLM
integrations.

---

## 2. Scope of the Current Iteration

Implemented now:

- Java 17 / Maven / Spring Boot 3.5.x project skeleton.
- PostgreSQL-backed JPA entities for patients, consultations, and prescription items.
- Bean-validated request DTOs and strict `ApiResponse<T>` JSON envelopes.
- Patient CRUD.
- Consultation ingestion from a raw transcript, SOAP extraction, prescription extraction,
  persistence, retrieval, editing, reprocessing, and deletion.
- Mock LLM extraction service implementing clinical-document heuristics.
- Mock Whisper transcription service with a multipart audio endpoint.
- Centralized exception handling with machine-readable error payloads.
- OpenAPI/Swagger UI, Actuator health/info, configurable CORS.
- Unit and integration tests using H2 in PostgreSQL compatibility mode.

Deliberately deferred:

- Real speech-to-text and LLM provider calls.
- Authentication/authorization, multi-tenancy, and audit trails.
- Audio object storage and asynchronous transcription jobs.
- Production database migrations, observability stack, and deployment manifests.
- Clinical review workflow and e-signature.

---

## 3. Architecture

### 3.1 Layering

```
HTTP client
   |
   v
controller  ->  dto (request validation / response mapping)
   |
   v
service     ->  LLMExtractionService (mock now, provider adapter later)
   |        ->  TranscriptionService (mock now, Whisper adapter later)
   v
repository  ->  JPA entities
   |
   v
PostgreSQL
```

Cross-cutting concerns:

- `exception` — domain exceptions plus `GlobalExceptionHandler` for consistent errors.
- `config` — typed application properties, CORS, OpenAPI.
- `dto` — immutable records; request records carry Jakarta Bean Validation annotations.

### 3.2 Package layout

```
com.scribe
├── AiClinicalScribeApplication
├── config
│   ├── AppProperties
│   ├── OpenApiConfig
│   └── WebConfig
├── controller
│   ├── ConsultationController
│   ├── PatientController
│   └── TranscriptionController
├── dto
│   ├── ApiResponse
│   ├── ConsultationRequest / ConsultationResponse
│   ├── ExtractedMedication / SoapExtractionResult
│   ├── PatientRequest / PatientResponse
│   ├── PrescriptionItemRequest / PrescriptionItemResponse
│   ├── TranscriptionResponse
│   ├── UpdateConsultationRequest
│   ├── FieldViolation / ValidationErrorPayload
├── exception
│   ├── BadRequestException
│   ├── ConflictException
│   ├── GlobalExceptionHandler
│   └── ResourceNotFoundException
├── model
│   ├── ConsultationEntity
│   ├── Gender
│   ├── PatientEntity
│   └── PrescriptionItemEntity
├── repository
│   ├── ConsultationRepository
│   └── PatientRepository
└── service
    ├── ConsultationService
    ├── LLMExtractionService
    ├── MockLlmExtractionService
    ├── MockWhisperTranscriptionService
    ├── PatientService
    └── TranscriptionService
```

### 3.3 Request flow — create consultation

1. `POST /api/v1/consultations` receives `patientId` and `rawTranscript`.
2. Bean Validation checks required fields and payload bounds.
3. `ConsultationService` verifies the patient exists.
4. `LLMExtractionService.extract(transcript)` returns `SoapExtractionResult` (four SOAP sections
   plus a medication list).
5. The service builds `ConsultationEntity`, attaches `PrescriptionItemEntity` children, and saves
   them in one transaction (cascade + orphan removal).
6. `ConsultationResponse` is returned inside the standard response envelope.

### 3.4 Request flow — audio

1. `POST /api/v1/transcriptions` accepts multipart field `audio`.
2. The current mock validates file presence, content type, and extension.
3. `MockWhisperTranscriptionService` returns a canned clinical dialogue for end-to-end demos.
4. The transcript can then be submitted to `POST /api/v1/consultations`.

The interface boundary (`TranscriptionService`) is intentionally the same shape a Whisper adapter
will implement.

---

## 4. Database Schema

Target database: PostgreSQL 16. JPA currently uses `ddl-auto=update` for local development.
Production should switch to Flyway/Liquibase migrations and `ddl-auto=validate`.

### 4.1 `patients`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `BIGINT` | PK, identity |
| `name` | `VARCHAR(120)` | not null |
| `age` | `INTEGER` | not null, 0–150 enforced at API layer |
| `gender` | `VARCHAR(20)` | not null, enum `MALE/FEMALE/OTHER/UNKNOWN` |
| `contact_number` | `VARCHAR(32)` | not null |
| `medical_history` | `TEXT` | nullable |
| `created_at` | `TIMESTAMPTZ` | not null, set by Hibernate |

Index: `idx_patients_name (name)`.

### 4.2 `consultations`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `BIGINT` | PK, identity |
| `patient_id` | `BIGINT` | not null; API validates patient existence |
| `raw_transcript` | `TEXT` | not null |
| `subjective` | `TEXT` | nullable |
| `objective` | `TEXT` | nullable |
| `assessment` | `TEXT` | nullable |
| `plan` | `TEXT` | nullable |
| `created_at` | `TIMESTAMPTZ` | not null, set by Hibernate |

Indexes: `idx_consultations_patient (patient_id)`, `idx_consultations_created_at (created_at)`.

`patient_id` is intentionally a scalar field in this iteration to match the domain contract. The
production migration should promote it to a real foreign key with
`ON DELETE RESTRICT` (or soft-delete semantics) and add a database-level referential guarantee.

### 4.3 `prescription_items`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `BIGINT` | PK, identity |
| `consultation_id` | `BIGINT` | not null, FK → `consultations.id`, `ON DELETE CASCADE` |
| `medicine_name` | `VARCHAR(160)` | not null |
| `dosage` | `VARCHAR(80)` | not null |
| `frequency` | `VARCHAR(80)` | not null |
| `duration` | `VARCHAR(80)` | not null |

Index: `idx_prescription_items_consultation (consultation_id)`.

Relationship: one consultation to many prescription items, managed bidirectionally in JPA with
cascade `ALL` and `orphanRemoval=true`.

### 4.4 Reference DDL

```sql
CREATE TABLE patients (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    age             INTEGER NOT NULL CHECK (age BETWEEN 0 AND 150),
    gender          VARCHAR(20) NOT NULL,
    contact_number  VARCHAR(32) NOT NULL,
    medical_history TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE consultations (
    id             BIGSERIAL PRIMARY KEY,
    patient_id     BIGINT NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    raw_transcript TEXT NOT NULL,
    subjective     TEXT,
    objective      TEXT,
    assessment     TEXT,
    plan           TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE prescription_items (
    id              BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
    medicine_name   VARCHAR(160) NOT NULL,
    dosage          VARCHAR(80) NOT NULL,
    frequency       VARCHAR(80) NOT NULL,
    duration        VARCHAR(80) NOT NULL
);

CREATE INDEX idx_patients_name ON patients(name);
CREATE INDEX idx_consultations_patient ON consultations(patient_id);
CREATE INDEX idx_consultations_created_at ON consultations(created_at);
CREATE INDEX idx_prescription_items_consultation ON prescription_items(consultation_id);
```

---

## 5. API Contract

Base path: `/api/v1`. Interactive docs: `/swagger-ui.html`; OpenAPI JSON: `/v3/api-docs`.

### 5.1 Response envelope

Every response uses the same strict wrapper:

```json
{
  "status": "SUCCESS",
  "message": "Human-readable outcome",
  "data": {},
  "timestamp": "2026-10-23T10:15:30Z"
}
```

- `status` is `SUCCESS` or `ERROR`.
- `message` is safe for direct display and logging.
- `data` holds the structured payload; it is `null` for operations with no payload.
- Validation failures put `{"code":"VALIDATION_FAILED","violations":[...]}` in `data`.

### 5.2 Endpoints

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/patients` | Create patient | `201` |
| `GET` | `/api/v1/patients` | List patients | `200` |
| `GET` | `/api/v1/patients/{id}` | Get patient | `200` |
| `PUT` | `/api/v1/patients/{id}` | Replace patient details | `200` |
| `DELETE` | `/api/v1/patients/{id}` | Delete patient (rejected if consultations exist) | `200` |
| `POST` | `/api/v1/consultations` | Extract SOAP + prescriptions and persist | `201` |
| `GET` | `/api/v1/consultations?patientId={id}` | List consultations, newest first | `200` |
| `GET` | `/api/v1/consultations/{id}` | Get one consultation | `200` |
| `PUT` | `/api/v1/consultations/{id}` | Clinician edits SOAP/replace prescriptions | `200` |
| `POST` | `/api/v1/consultations/{id}/reprocess` | Re-run extraction on stored transcript | `200` |
| `DELETE` | `/api/v1/consultations/{id}` | Delete consultation and items | `200` |
| `POST` | `/api/v1/transcriptions` | Multipart audio → transcript (mock) | `200` |

Error statuses: `400` validation/malformed input, `404` missing resource, `409` conflict,
`413` oversized upload, `500` unexpected failure.

### 5.3 Create consultation example

Request:

```json
{
  "patientId": 1,
  "rawTranscript": "Patient reports a sore throat and fever for three days. The temperature is 38.4 C and the throat is red. It looks like acute pharyngitis. I will prescribe amoxicillin 500 mg three times daily for 7 days. Follow up in one week."
}
```

Response (`201 Created`):

```json
{
  "status": "SUCCESS",
  "message": "Consultation processed and saved successfully",
  "data": {
    "id": 1,
    "patientId": 1,
    "rawTranscript": "Patient reports a sore throat and fever for three days. The temperature is 38.4 C and the throat is red. It looks like acute pharyngitis. I will prescribe amoxicillin 500 mg three times daily for 7 days. Follow up in one week.",
    "soapNote": {
      "subjective": "Patient reports a sore throat and fever for three days.",
      "objective": "The temperature is 38.4 C and the throat is red.",
      "assessment": "It looks like acute pharyngitis.",
      "plan": "I will prescribe amoxicillin 500 mg three times daily for 7 days. Follow up in one week."
    },
    "prescriptionItems": [
      {
        "id": 1,
        "consultationId": 1,
        "medicineName": "amoxicillin",
        "dosage": "500 mg",
        "frequency": "three times daily",
        "duration": "7 days"
      }
    ],
    "createdAt": "2026-10-23T10:15:30Z"
  },
  "timestamp": "2026-10-23T10:15:31Z"
}
```

---

## 6. Validation and Error Model

- Request records use Jakarta Bean Validation: `@NotNull`, `@NotBlank`, `@Min`, `@Max`,
  `@Positive`, `@Size`, `@Pattern`, and `@Valid` for nested lists.
- Path variables and query parameters are validated with `@Validated` plus `@Positive`.
- `GlobalExceptionHandler` maps domain exceptions and framework exceptions into the same
  `ApiResponse` envelope, so clients never receive inconsistent error bodies.
- Unexpected exceptions are logged with stack traces and returned as a generic `500` message; no
  internal details or PHI leak into responses.

---

## 7. AI Integration Design

### 7.1 Current abstraction

```java
public interface LLMExtractionService {
    SoapExtractionResult extract(String rawTranscript);
}
```

`SoapExtractionResult` contains `subjective`, `objective`, `assessment`, `plan`, and
`List<ExtractedMedication>`. The interface is provider-agnostic and returns a validated domain
object rather than raw JSON.

`MockLlmExtractionService` (active when `app.ai.provider=mock`) classifies sentences with keyword
heuristics and extracts medications with a deterministic regex. Selected with
`@ConditionalOnProperty`, so a real adapter can be enabled without changing callers.

`TranscriptionService` has the same boundary for audio:

```java
public interface TranscriptionService {
    String transcribe(MultipartFile audio);
}
```

### 7.2 Replacing the mock LLM

1. Add a provider adapter, e.g. `OpenAiLlmExtractionService implements LLMExtractionService`.
2. Enable it with `@ConditionalOnProperty(name="app.ai.provider", havingValue="openai")`.
3. Compose a clinical extraction prompt with:
   - role and safety constraints (no diagnosis invention, preserve negations);
   - few-shot SOAP examples;
   - explicit output JSON schema;
   - instruction to return an empty medication list when none is stated.
4. Use the provider's structured-output/JSON-schema feature where available; otherwise parse and
   validate with Jackson.
5. Validate output: non-null SOAP fields, bounded lengths, medication name/dose sanity, no
   hallucinated items. On validation failure, retry once and then return a degraded result that
   requires clinician review.
6. Add timeouts, retry with jitter, circuit breaker (Resilience4j), token accounting, and
   provider latency/error metrics.
7. Persist extraction metadata (provider, model, prompt version, latency, token counts,
   confidence) in an `extraction_runs` table for auditability and reproducibility.

### 7.3 Replacing the mock Whisper STT

1. Add `OpenAiWhisperTranscriptionService` (or an adapter for self-hosted `faster-whisper`).
2. Keep the controller contract unchanged; the adapter streams the multipart file to the provider.
3. For long recordings, upload audio to object storage (S3/GCS) and process asynchronously:
   - return `202 Accepted` with a job ID;
   - expose `GET /api/v1/transcriptions/{jobId}` for status/result;
   - persist job state and errors.
4. Add language hints, diarization (speaker labels), timestamps, and medical vocabulary prompts.
5. Enforce upload limits, MIME sniffing, malware scanning, and retention policies.

### 7.4 Suggested production flow

```
audio upload -> object storage -> STT job -> transcript
    -> LLM extraction -> schema validation -> clinician review -> signed encounter
```

Human review is required for any AI-generated clinical content. The API should eventually expose a
review state (`DRAFT`, `REVIEWED`, `SIGNED`) rather than treating extraction as final.

---

## 8. Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ai_clinical_scribe` | JDBC URL |
| `DB_USERNAME` | `scribe` | DB user |
| `DB_PASSWORD` | `scribe` | DB password |
| `DB_POOL_SIZE` | `10` | Hikari max pool size |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `JPA_SHOW_SQL` | `false` | SQL logging |
| `SERVER_PORT` | `8080` | HTTP port |
| `AI_PROVIDER` | `mock` | LLM adapter selector |
| `AI_MODEL` | `mock-soap-extractor-v1` | Model label |
| `WHISPER_PROVIDER` | `mock` | STT adapter selector |
| `CORS_ALLOWED_ORIGINS` | local dev origins | Comma-separated allowed origins |
| `LOG_LEVEL` | `INFO` | `com.scribe` log level |

---

## 9. Testing Strategy

Current:

- `MockLlmExtractionServiceTest` — SOAP classification, medication extraction, empty fallbacks.
- `ConsultationServiceTest` — persistence orchestration and unknown-patient rejection.
- `ConsultationApiIntegrationTest` — patient creation, consultation generation, validation errors.
- `AiClinicalScribeApplicationTests` — context load with H2 in PostgreSQL mode.

Next:

- Testcontainers PostgreSQL integration tests.
- Contract tests for the strict response envelope.
- Provider adapter tests using WireMock (timeouts, malformed JSON, rate limiting).
- Security tests once authentication is added.
- Property-based tests for transcript parsing and validation boundaries.

---

## 10. Security and Compliance Considerations

This project is a development scaffold, not a compliance-certified product.

- Do not log raw transcripts, audio, or extracted PHI in production logs.
- Add authentication/authorization (OAuth2/OIDC, clinician roles), tenant isolation, and
  field-level access control.
- Encrypt data in transit and at rest; use secrets management rather than environment defaults.
- Add immutable audit logs for access, edits, AI runs, and signatures.
- Define retention, deletion, and export policies for audio/transcripts.
- Sign BAAs with providers and confirm data-processing regions before sending PHI to an LLM or STT
  API; prefer providers with no-training guarantees.
- Show disclaimers and require clinician review before clinical use.

---

## 11. Roadmap

### Phase 1 — Foundation (current)

- [x] Project skeleton, dependencies, package structure.
- [x] Entities, repositories, DTOs, validation, response envelope.
- [x] Consultation pipeline with mock extraction.
- [x] Mock STT endpoint.
- [x] Error handling, OpenAPI, Actuator, tests.

### Phase 2 — Production data layer

- [ ] Flyway migrations and `ddl-auto=validate`.
- [ ] Real FK from `consultations.patient_id` to `patients.id`.
- [ ] Optimistic locking (`@Version`) and soft deletes.
- [ ] Pagination and filtering for consultation lists.
- [ ] Testcontainers PostgreSQL tests.

### Phase 3 — Real AI integrations

- [ ] OpenAI/Azure/Anthropic LLM adapter with structured outputs.
- [ ] Whisper API or self-hosted `faster-whisper` adapter.
- [ ] Async transcription jobs and object storage.
- [ ] Extraction metadata/audit table and confidence scoring.
- [ ] Retries, circuit breakers, timeouts, observability.
- [ ] Clinical review/sign-off workflow.

### Phase 4 — Enterprise hardening

- [ ] OIDC authentication and role-based access.
- [ ] Audit logging, rate limiting, request tracing, dashboards/alerts.
- [ ] PHI redaction before third-party calls.
- [ ] CI/CD, container image, Kubernetes/Helm deployment, backups/DR.
- [ ] Load, security, and disaster-recovery testing.

---

## 12. Open Questions

- Which LLM/STT providers and hosting regions are approved for PHI?
- Is clinician review mandatory before prescriptions are released?
- Should audio be retained, and for how long?
- Are speaker diarization and multilingual consultations required?
- Does the platform need EHR/FHIR integration (e.g., `Composition`, `MedicationRequest`)?
