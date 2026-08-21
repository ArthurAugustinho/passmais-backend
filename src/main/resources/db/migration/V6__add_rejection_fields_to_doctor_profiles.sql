ALTER TABLE doctor_profiles
    ADD COLUMN rejected_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN failure_description VARCHAR(500);
