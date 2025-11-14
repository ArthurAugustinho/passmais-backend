-- Track cancel metadata for appointments
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS canceled_reason VARCHAR(500);
