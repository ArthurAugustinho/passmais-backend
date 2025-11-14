-- Extend users for email verification
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS verification_code VARCHAR(64);

-- Extend doctor_profiles for required registration fields
ALTER TABLE doctor_profiles
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(14),
    ADD COLUMN IF NOT EXISTS birth_date DATE,
    ADD COLUMN IF NOT EXISTS photo_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS consultation_price NUMERIC(12,2);

-- Constraints for uniqueness
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_doctor_profiles_cpf'
    ) THEN
        ALTER TABLE doctor_profiles ADD CONSTRAINT uk_doctor_profiles_cpf UNIQUE (cpf);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_doctor_profiles_crm'
    ) THEN
        ALTER TABLE doctor_profiles ADD CONSTRAINT uk_doctor_profiles_crm UNIQUE (crm);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_doctor_profiles_phone'
    ) THEN
        ALTER TABLE doctor_profiles ADD CONSTRAINT uk_doctor_profiles_phone UNIQUE (phone);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_patient_profiles_cpf'
    ) THEN
        ALTER TABLE patient_profiles ADD CONSTRAINT uk_patient_profiles_cpf UNIQUE (cpf);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_patient_profiles_cell_phone'
    ) THEN
        ALTER TABLE patient_profiles ADD CONSTRAINT uk_patient_profiles_cell_phone UNIQUE (cell_phone);
    END IF;
END$$;
