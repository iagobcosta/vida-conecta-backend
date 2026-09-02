ALTER TABLE appointments
    ADD COLUMN cancel_reason VARCHAR(500),
    ADD COLUMN cancelled_by UUID REFERENCES users (id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users (id),
    type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    appointment_id UUID REFERENCES appointments (id),
    action_path VARCHAR(200),
    action_label VARCHAR(80),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_id, created_at DESC);
