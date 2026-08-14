BEGIN;

SET LOCAL search_path = public, extensions, pg_catalog;

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE patients
SET gioi_tinh = CASE public.normalize_vietnamese(trim(gioi_tinh))
    WHEN 'nam' THEN 'Nam'
    WHEN 'nu' THEN 'Nữ'
    WHEN 'khac' THEN 'Khác'
    ELSE 'Khác'
END
WHERE gioi_tinh NOT IN ('Nam', 'Nữ', 'Khác');

ALTER TABLE patients
    DROP CONSTRAINT IF EXISTS chk_patients_gioi_tinh;

ALTER TABLE patients
    ADD CONSTRAINT chk_patients_gioi_tinh
    CHECK (gioi_tinh IN ('Nam', 'Nữ', 'Khác'));

CREATE INDEX IF NOT EXISTS idx_patients_active_search
    ON patients USING GIN (ho_va_ten_search gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_patients_deleted_at
    ON patients (deleted_at DESC)
    WHERE deleted_at IS NOT NULL;

COMMIT;
