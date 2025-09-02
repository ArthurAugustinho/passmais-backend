-- Inicialização do schema Passmais
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    lgpd_accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP WITH TIME ZONE,
    last_token_revalidated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE patient_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    cpf VARCHAR(14) NOT NULL,
    birth_date DATE,
    address VARCHAR(255),
    cell_phone VARCHAR(20),
    communication_preference VARCHAR(30)
);

CREATE TABLE dependents (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    birth_date DATE,
    relation VARCHAR(30)
);

CREATE TABLE clinics (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    cnpj VARCHAR(18) NOT NULL,
    address VARCHAR(255),
    approved BOOLEAN NOT NULL DEFAULT false,
    approved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE doctor_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    crm VARCHAR(30) NOT NULL,
    specialty VARCHAR(80),
    bio VARCHAR(500),
    approved BOOLEAN NOT NULL DEFAULT false,
    approved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE doctor_clinics (
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, clinic_id)
);

CREATE TABLE availabilities (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    day_of_week VARCHAR(12) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    dependent_id UUID REFERENCES dependents(id) ON DELETE SET NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    observations VARCHAR(500),
    rescheduled_from_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,
    value NUMERIC(12,2)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    method VARCHAR(20) NOT NULL,
    value NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    receipt VARCHAR(255),
    paid_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    rating INT NOT NULL,
    comment VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    moderated_by_id UUID REFERENCES users(id) ON DELETE SET NULL,
    moderated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE clinical_notes (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    notes VARCHAR(3000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    content VARCHAR(500) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    details VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE moderation_logs (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    details VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

