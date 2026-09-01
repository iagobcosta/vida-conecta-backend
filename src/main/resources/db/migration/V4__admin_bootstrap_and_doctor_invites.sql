CREATE TABLE admin_bootstrap_tokens (
    id UUID PRIMARY KEY,
    token UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

INSERT INTO admin_bootstrap_tokens (id, token, created_at)
VALUES (
    'a1111111-1111-4111-8111-111111111111',
    'b2222222-2222-4222-8222-222222222222',
    NOW()
);

CREATE TABLE admin_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id),
    full_name VARCHAR(255) NOT NULL
);

CREATE TABLE doctor_invites (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    token UUID NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_doctor_invites_email ON doctor_invites (email);
