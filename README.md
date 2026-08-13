# Dental Patient Records

## Overview

Dental Patient Records is a JavaFX desktop application for managing dental patient information, visit histories, and revenue entries associated with visits. It is a patient-record system, not an appointment scheduler.

## Technology

- Java 21
- JavaFX 21
- Maven
- PostgreSQL
- Supabase PostgreSQL
- PostgreSQL JDBC
- JUnit 5

## Architecture

The presentation layer follows MVC, and controllers communicate with the database only through the application and persistence layers:

```text
View / FXML
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Supabase PostgreSQL
```

FXML and JavaFX CSS define the views. Controllers coordinate UI actions, services provide validation and workflows, and JDBC repositories own persistence logic.

## Database

The primary relationship is:

```text
patients 1 ── N visits 1 ── N doanh_thu
```

- One patient can have multiple visits.
- One visit can have multiple revenue rows.
- Revenue totals are calculated dynamically from monetary entries using `BigDecimal` and are not stored as a separate total field.
- VAT and personal-income-tax fields are intentionally not stored in the domain model or database.

The schema definition is available in `src/main/resources/db/migration/V1__create_dental_patient_schema.sql`.

## Environment Setup

Configure these environment variables before running the application or database-backed tests:

- `SUPABASE_DB_HOST`
- `SUPABASE_DB_PORT`
- `SUPABASE_DB_NAME`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`

See `database.properties.example` for placeholder names only. The application reads credentials from environment variables; it does not load that example file directly.

## Running

With Maven available on `PATH`:

```shell
mvn clean test
mvn javafx:run
```

IntelliJ IDEA's bundled Maven can run the same lifecycle goals and JavaFX goal when system Maven is unavailable.

Database-backed tests use isolated transactions and roll back their test records.

## Current Development Progress

Development phases 1–10 are represented in the current codebase:

- Maven, Java 21, JavaFX, PostgreSQL configuration, and schema
- Patient, visit, and revenue domain models
- JDBC repositories and service-layer validation
- JavaFX MVC shell, patient directory, patient details, and navigation
- Patient create/edit/search workflows
- Visit history and atomic visit/revenue create and edit workflows
- Dynamic `BigDecimal` revenue totals and cascade-aware visit deletion
- Automated repository, service, transaction-workflow, FXML, and JavaFX startup tests

The next intended development phase is Phase 11. Its exact report/printing scope should be confirmed from the project plan before implementation; report generation and printing are not implemented in this repository state.

## Security

- Never commit real Supabase credentials, passwords, tokens, or private keys.
- Keep database configuration in the required environment variables.
- Do not commit `.env`, `database.properties`, local secret property files, IDE metadata, or Maven build output.
