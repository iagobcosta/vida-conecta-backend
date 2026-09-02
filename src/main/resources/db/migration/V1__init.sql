CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE patient_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id),
    full_name VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    phone VARCHAR(32)
);

CREATE TABLE doctor_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id),
    full_name VARCHAR(255) NOT NULL,
    crm VARCHAR(32) NOT NULL UNIQUE,
    specialty VARCHAR(128) NOT NULL
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id),
    doctor_id UUID NOT NULL REFERENCES users (id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_appointments_doctor_time ON appointments (doctor_id, scheduled_at);
CREATE INDEX idx_appointments_patient ON appointments (patient_id);

CREATE TABLE consents (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id),
    doctor_id UUID NOT NULL REFERENCES users (id),
    scope VARCHAR(32) NOT NULL,
    appointment_id UUID REFERENCES appointments (id),
    version INTEGER NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_consents_patient_doctor ON consents (patient_id, doctor_id);

CREATE TABLE clinical_notes (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id),
    author_doctor_id UUID NOT NULL REFERENCES users (id),
    appointment_id UUID REFERENCES appointments (id),
    ciphertext BYTEA NOT NULL,
    iv BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_clinical_notes_patient ON clinical_notes (patient_id);

CREATE TABLE ehr_access_audit (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users (id),
    patient_id UUID NOT NULL REFERENCES users (id),
    appointment_id UUID,
    action VARCHAR(32) NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ehr_audit_patient ON ehr_access_audit (patient_id, accessed_at DESC);

CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id),
    doctor_id UUID NOT NULL REFERENCES users (id),
    appointment_id UUID NOT NULL REFERENCES appointments (id),
    issued_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL REFERENCES prescriptions (id) ON DELETE CASCADE,
    medication VARCHAR(255) NOT NULL,
    dosage VARCHAR(128) NOT NULL,
    instructions VARCHAR(512) NOT NULL
);
