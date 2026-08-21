ALTER TABLE team_invites
    ADD COLUMN IF NOT EXISTS display_code VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_invites_display_code
    ON team_invites (display_code)
    WHERE display_code IS NOT NULL;
