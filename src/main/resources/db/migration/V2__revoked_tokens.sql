CREATE TABLE IF NOT EXISTS revoked_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(1024) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_revoked_tokens_token ON revoked_tokens(token);

