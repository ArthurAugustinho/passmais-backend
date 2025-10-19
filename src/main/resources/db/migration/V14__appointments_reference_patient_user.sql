-- Migrate appointments to reference patients via users.id instead of patient_profiles.id
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS patient_user_id UUID;

UPDATE appointments a
SET patient_user_id = p.user_id
FROM patient_profiles p
WHERE a.patient_user_id IS NULL
  AND a.patient_id = p.id;

ALTER TABLE appointments
    ALTER COLUMN patient_user_id SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'appointments_patient_id_fkey'
          AND table_name = 'appointments'
    ) THEN
        ALTER TABLE appointments DROP CONSTRAINT appointments_patient_id_fkey;
    END IF;
END $$;

ALTER TABLE appointments DROP COLUMN patient_id;
ALTER TABLE appointments RENAME COLUMN patient_user_id TO patient_id;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_patient_user
        FOREIGN KEY (patient_id)
        REFERENCES users(id)
        ON DELETE CASCADE;
