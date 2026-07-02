-- Account data requests table for LGPD/privacy requests
CREATE TABLE account_data_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_subject VARCHAR(255) NOT NULL,
    username VARCHAR(100),
    email VARCHAR(255),
    display_name VARCHAR(200),
    request_type VARCHAR(30) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_note TEXT
);

CREATE INDEX idx_account_data_request_user_subject ON account_data_request(user_subject);
CREATE INDEX idx_account_data_request_status ON account_data_request(status);
