ALTER TABLE patient_files
    ADD COLUMN IF NOT EXISTS presence_confirmed_at TIMESTAMP WITH TIME ZONE;
