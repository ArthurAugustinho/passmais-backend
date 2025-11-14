CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE team_invites
    ADD COLUMN invite_code_hash VARCHAR(128),
    ADD COLUMN uses_remaining INT,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();

UPDATE team_invites
SET invite_code_hash = encode(digest(code, 'sha256'), 'hex');

UPDATE team_invites
SET uses_remaining = GREATEST(max_uses - uses, 0);

UPDATE team_invites
SET status = CASE
    WHEN active IS FALSE THEN 'REVOKED'
    WHEN uses_remaining = 0 THEN 'EXHAUSTED'
    WHEN expires_at < now() THEN 'EXPIRED'
    ELSE 'ACTIVE'
END;

ALTER TABLE team_invites
    ALTER COLUMN invite_code_hash SET NOT NULL,
    ADD CONSTRAINT uk_team_invites_code_hash UNIQUE (invite_code_hash);

ALTER TABLE team_invites
    ALTER COLUMN uses_remaining SET NOT NULL;

ALTER TABLE team_invites
    DROP COLUMN code,
    DROP COLUMN uses,
    DROP COLUMN active;

ALTER TABLE team_invites
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE team_invites
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE TABLE invite_audit_logs (
    id UUID PRIMARY KEY,
    invite_code_id UUID NOT NULL REFERENCES team_invites(id) ON DELETE CASCADE,
    used_by_secretary_id UUID REFERENCES users(id) ON DELETE SET NULL,
    used_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(20) NOT NULL,
    details VARCHAR(200)
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(14);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_cpf'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_cpf UNIQUE (cpf);
    END IF;
END$$;
