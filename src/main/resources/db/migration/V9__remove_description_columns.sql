-- Remove description columns from promotion table
-- The dedup index (uq_promotion_dedup) was already simplified in V8 to (normalized_url, created_date)
-- so it does not reference normalized_description anymore.

DROP INDEX IF EXISTS idx_promotion_normalized_desc;

ALTER TABLE promotion DROP COLUMN IF EXISTS description;
ALTER TABLE promotion DROP COLUMN IF EXISTS normalized_description;
