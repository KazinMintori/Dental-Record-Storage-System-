BEGIN;

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA extensions;
SET LOCAL search_path = public, extensions, pg_catalog;

CREATE OR REPLACE FUNCTION public.normalize_vietnamese(value TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
STRICT
PARALLEL SAFE
SET search_path = pg_catalog, extensions, public
AS $$
    SELECT lower(unaccent(value))
$$;

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS so_dien_thoai VARCHAR(30);

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS ho_va_ten_search TEXT
        GENERATED ALWAYS AS (public.normalize_vietnamese(ho_va_ten)) STORED;

CREATE INDEX IF NOT EXISTS idx_patients_ho_va_ten_search
    ON patients USING GIN (ho_va_ten_search gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_patients_so_dien_thoai
    ON patients (so_dien_thoai);

COMMIT;
