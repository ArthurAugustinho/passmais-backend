-- Add consultation detail columns to appointments
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS symptom_duration VARCHAR(120);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS pre_consult_notes TEXT;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS finalized_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS updated_by_user_id UUID;

DO $$
BEGIN
    ALTER TABLE appointments
        ADD CONSTRAINT fk_appointments_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id) ON DELETE SET NULL;
EXCEPTION
    WHEN duplicate_object THEN
        -- constraint already exists
        NULL;
END;
$$;

-- Consultation records table to support autosave and finalization
CREATE TABLE IF NOT EXISTS consultation_records (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    symptom_duration VARCHAR(120),
    anamnesis TEXT,
    physical_exam TEXT,
    plan TEXT,
    last_saved_at TIMESTAMP WITH TIME ZONE,
    updated_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consultation_records_updated_by ON consultation_records(updated_by_user_id);

-- Ensure every existing appointment has a record entry
INSERT INTO consultation_records (id, appointment_id, status, last_saved_at, created_at, updated_at)
SELECT uuid_generate_v4(), a.id, 'DRAFT', a.updated_at, now(), now()
FROM appointments a
WHERE NOT EXISTS (
    SELECT 1 FROM consultation_records cr WHERE cr.appointment_id = a.id
);

-- Patient alerts table for aggregated clinical metadata
CREATE TABLE IF NOT EXISTS patient_alerts (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    label VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_patient_alerts_patient ON patient_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_alerts_active ON patient_alerts(active);
