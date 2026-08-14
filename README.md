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
- HikariCP connection pool
- Apache PDFBox 3 (Unicode PDF reports)
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

Schema migrations are available in `src/main/resources/db/migration`:

- `V1__create_dental_patient_schema.sql` creates a new database.
- `V2__patient_search_and_phone.sql` upgrades an existing database with patient phone numbers,
  accent-insensitive Vietnamese name normalization, and search indexes.
- `V3__patient_trash_and_gender.sql` adds soft deletion/trash, normalizes existing gender values,
  restricts gender to `Nam`, `Nữ`, or `Khác`, and adds active/trash indexes.
- `V4__patient_pagination_indexes_and_optional_revenue.sql` adds paging/search indexes and makes
  revenue reference/description fields optional.

For an existing Supabase project, run V2, V3, and then V4 once in the Supabase SQL Editor before
starting this version. New databases only need V1 because it already contains the current schema.

## Environment Setup

Configure these environment variables before running the application or database-backed tests:

- `SUPABASE_DB_HOST`
- `SUPABASE_DB_PORT`
- `SUPABASE_DB_NAME`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`

Optional report metadata and PDF settings:

- `DENTAL_CLINIC_NAME`
- `DENTAL_CLINIC_ADDRESS`
- `DENTAL_CLINIC_TAX_CODE`
- `DENTAL_PDF_FONT` and `DENTAL_PDF_BOLD_FONT` (paths to Unicode TTF files; Windows Arial/Segoe UI are detected automatically)

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
- Patient create/edit workflows with Unicode-preserving phone and demographic data
- Debounced database-side Vietnamese name search, combined advanced filters, and patient table
- Shared pooled Supabase connections, a 250 ms search debounce, and indexed active-record searches
- Consistent `Ngày/Tháng/Năm` display/input and constrained `Nam`/`Nữ`/`Khác` gender selectors
- Recoverable patient deletion through a trash view, with restore and guarded permanent deletion
- A patient-detail loading view that remains visible for at least 450 ms and waits for visit data
- Strictly isolated visit-editor state per patient, with stale asynchronous results discarded
- Inline visit/revenue validation for required fields, date format, future dates, and monetary values
- Visit history and atomic visit/revenue create and edit workflows
- Dynamic `BigDecimal` revenue totals and cascade-aware visit deletion
- Preview and Unicode PDF export for the 14-column medical book and portrait revenue book
- The medical-book preview freezes `TT` and `Họ và tên` while the remaining columns scroll horizontally
- The revenue PDF follows the paper `S2-HKD` form, retaining dotted placeholders and manual tax/signature fields
- Report ranges based exclusively on the exact `visits.created_at` timestamp; the selected end date is inclusive
- Automated repository, service, transaction-workflow, FXML, and JavaFX startup tests

## Security

- Never commit real Supabase credentials, passwords, tokens, or private keys.
- Keep database configuration in the required environment variables.
- Do not commit `.env`, `database.properties`, local secret property files, IDE metadata, or Maven build output.
