-- Admin import support columns
ALTER TABLE promotion ADD COLUMN publish_at TIMESTAMPTZ;
ALTER TABLE promotion ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE promotion ADD COLUMN source VARCHAR(50);
ALTER TABLE promotion ADD COLUMN source_id VARCHAR(200);
ALTER TABLE promotion ADD COLUMN batch_id VARCHAR(200);
ALTER TABLE promotion ADD COLUMN author_username VARCHAR(100);

-- Backfill publish_at so existing promotions remain visible in public queries
UPDATE promotion SET publish_at = created_at WHERE publish_at IS NULL;

-- Make publish_at NOT NULL after backfill
ALTER TABLE promotion ALTER COLUMN publish_at SET NOT NULL;

-- Indexes for admin import operations
CREATE UNIQUE INDEX uq_promotion_source_id ON promotion(source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_promotion_batch_id ON promotion(batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_promotion_publish_at ON promotion(status, publish_at);
