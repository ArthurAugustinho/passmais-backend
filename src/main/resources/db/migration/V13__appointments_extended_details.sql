-- Extend appointments table with patient snapshot and payment details
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS booked_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS patient_full_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS patient_cpf VARCHAR(14),
    ADD COLUMN IF NOT EXISTS patient_birth_date DATE,
    ADD COLUMN IF NOT EXISTS patient_cell_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS location VARCHAR(160),
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(40);
