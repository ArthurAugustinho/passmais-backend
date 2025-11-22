CREATE TABLE doctor_schedule_state (
    doctor_id UUID PRIMARY KEY REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    mode VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE doctor_schedule_specific_settings (
    doctor_id UUID PRIMARY KEY REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    appointment_interval INTEGER NOT NULL,
    buffer_minutes INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE doctor_schedule_specific (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    schedule_date DATE NOT NULL,
    slots JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (doctor_id, schedule_date)
);

CREATE INDEX idx_doctor_schedule_specific_doctor_date ON doctor_schedule_specific (doctor_id, schedule_date);

CREATE TABLE doctor_schedule_recurring_settings (
    doctor_id UUID PRIMARY KEY REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    appointment_interval INTEGER NOT NULL,
    buffer_minutes INTEGER NOT NULL,
    start_date DATE,
    end_date DATE,
    no_end_date BOOLEAN NOT NULL DEFAULT false,
    enabled BOOLEAN NOT NULL DEFAULT false,
    is_recurring_active BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE doctor_schedule_recurring (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    weekday VARCHAR(12) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    slots JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (doctor_id, weekday)
);

CREATE INDEX idx_doctor_schedule_recurring_doctor_weekday ON doctor_schedule_recurring (doctor_id, weekday);

CREATE TABLE doctor_schedule_exceptions (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    exception_date DATE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_doctor_schedule_exceptions_doctor_date ON doctor_schedule_exceptions (doctor_id, exception_date);

CREATE TABLE doctor_available_slots (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    start_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (doctor_id, start_at_utc)
);

CREATE INDEX idx_doctor_available_slots_doctor_date ON doctor_available_slots (doctor_id, slot_date);

CREATE TABLE schedule_audit_log (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    change_type VARCHAR(20) NOT NULL,
    old_schedule JSONB,
    new_schedule JSONB,
    changed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedule_audit_log_doctor ON schedule_audit_log (doctor_id);
