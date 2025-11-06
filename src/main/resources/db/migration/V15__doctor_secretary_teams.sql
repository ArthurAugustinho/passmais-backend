CREATE TABLE team_invites (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL UNIQUE,
    max_uses INT NOT NULL,
    uses INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_team_invites_doctor_id
    ON team_invites (doctor_id);

CREATE TABLE doctor_secretaries (
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    secretary_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (doctor_id, secretary_id)
);

CREATE INDEX IF NOT EXISTS idx_doctor_secretaries_secretary_id
    ON doctor_secretaries (secretary_id);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
