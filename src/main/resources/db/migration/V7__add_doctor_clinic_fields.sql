ALTER TABLE doctor_profiles
    ADD COLUMN IF NOT EXISTS clinic_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS clinic_street_number VARCHAR(160),
    ADD COLUMN IF NOT EXISTS clinic_city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS clinic_postal_code VARCHAR(20);
