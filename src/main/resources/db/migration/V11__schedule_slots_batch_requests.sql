CREATE TABLE schedule_slots (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    slot_time TIME,
    source VARCHAR(20) NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES doctor_profiles(id) ON DELETE SET NULL,
    delete_reason VARCHAR(120)
);

CREATE INDEX idx_schedule_slots_active_doctor_date
    ON schedule_slots (doctor_id, slot_date)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_schedule_slots_active_doctor_date_time
    ON schedule_slots (doctor_id, slot_date, slot_time)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_schedule_slots_active_blocked_day
    ON schedule_slots (doctor_id, slot_date)
    WHERE deleted_at IS NULL AND slot_time IS NULL;

CREATE INDEX idx_schedule_slots_active_cover
    ON schedule_slots (doctor_id, slot_date, slot_time)
    INCLUDE (source, version)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_schedule_slots_brin_date
    ON schedule_slots USING BRIN (slot_date);

CREATE TABLE schedule_batch_requests (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    idempotency_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_payload JSONB NOT NULL,
    http_status INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (doctor_id, idempotency_key)
);

CREATE INDEX idx_schedule_batch_requests_doctor_created
    ON schedule_batch_requests (doctor_id, created_at DESC);
