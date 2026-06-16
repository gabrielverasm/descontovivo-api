CREATE TABLE moderation_log (
    id UUID PRIMARY KEY,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    actor VARCHAR(100) NOT NULL DEFAULT 'admin',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_moderation_log_target ON moderation_log(target_type, target_id);
