-- A rolling 24-hour window cannot be represented by the previous lifetime/source
-- and calendar-day/URL unique indexes. The application enforces the temporal rule.
DROP INDEX IF EXISTS uq_promotion_source_id;
DROP INDEX IF EXISTS uq_promotion_dedup;

CREATE INDEX idx_promotion_source_id ON promotion(source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_promotion_normalized_url_created_date ON promotion(normalized_url, created_date);
