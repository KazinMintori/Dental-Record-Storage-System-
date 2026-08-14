BEGIN;

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA extensions;
SET LOCAL search_path = public, extensions, pg_catalog;

ALTER TABLE doanh_thu
    ALTER COLUMN dien_giai DROP NOT NULL;

-- Accent-insensitive partial-name search on active patient records.
CREATE INDEX IF NOT EXISTS idx_patients_active_search
    ON patients USING GIN (ho_va_ten_search gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Contains-search for normalized phone numbers (spaces are ignored by the query).
CREATE INDEX IF NOT EXISTS idx_patients_active_phone_search
    ON patients USING GIN ((replace(coalesce(so_dien_thoai, ''), ' ', '')) gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- The primary key already indexes id; this smaller partial index serves active-record paging/code lookup.
CREATE INDEX IF NOT EXISTS idx_patients_active_id
    ON patients (id)
    WHERE deleted_at IS NULL;

ANALYZE patients;

COMMIT;
