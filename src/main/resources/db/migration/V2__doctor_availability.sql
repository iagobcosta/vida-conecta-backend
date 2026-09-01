CREATE TABLE doctor_availabilities (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES users (id),
    day_of_week VARCHAR(16) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_doctor_availabilities_doctor ON doctor_availabilities (doctor_id, day_of_week);
