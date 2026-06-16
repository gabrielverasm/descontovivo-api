-- Add engagement counters to promotion
ALTER TABLE promotion ADD COLUMN likes_count INT NOT NULL DEFAULT 0;
ALTER TABLE promotion ADD COLUMN dislikes_count INT NOT NULL DEFAULT 0;
ALTER TABLE promotion ADD COLUMN comments_count INT NOT NULL DEFAULT 0;

-- Votes table
CREATE TABLE promotion_vote (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotion(id),
    client_id VARCHAR(120) NOT NULL,
    vote_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(promotion_id, client_id)
);

CREATE INDEX idx_promotion_vote_promotion ON promotion_vote(promotion_id);

-- Comments table
CREATE TABLE promotion_comment (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotion(id),
    parent_id UUID REFERENCES promotion_comment(id),
    client_id VARCHAR(120) NOT NULL,
    author_name VARCHAR(120) NOT NULL,
    content TEXT,
    removed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    removed_at TIMESTAMPTZ
);

CREATE INDEX idx_promotion_comment_promotion ON promotion_comment(promotion_id, created_at);
CREATE INDEX idx_promotion_comment_parent ON promotion_comment(parent_id);
