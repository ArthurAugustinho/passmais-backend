ALTER TABLE team_invites
    ADD COLUMN secretary_full_name VARCHAR(255),
    ADD COLUMN secretary_corporate_email VARCHAR(255);

UPDATE team_invites
SET secretary_full_name = COALESCE(secretary_full_name, ''),
    secretary_corporate_email = COALESCE(secretary_corporate_email, '');

ALTER TABLE team_invites
    ALTER COLUMN secretary_full_name SET NOT NULL,
    ALTER COLUMN secretary_corporate_email SET NOT NULL;

ALTER TABLE team_invites
    ALTER COLUMN max_uses SET DEFAULT 1,
    ALTER COLUMN uses_remaining SET DEFAULT 1;

UPDATE team_invites
SET max_uses = 1
WHERE max_uses IS DISTINCT FROM 1;

UPDATE team_invites
SET uses_remaining = LEAST(uses_remaining, 1);

UPDATE team_invites
SET status = 'REVOKED'
WHERE uses_remaining = 0
  AND status IN ('ACTIVE', 'EXHAUSTED');
