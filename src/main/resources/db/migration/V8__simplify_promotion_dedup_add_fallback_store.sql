-- Simplify dedup index: remove normalized_description from uniqueness check
DROP INDEX IF EXISTS uq_promotion_dedup;
CREATE UNIQUE INDEX uq_promotion_dedup ON promotion(normalized_url, created_date);

-- Insert fallback store (idempotent)
INSERT INTO store (id, name, slug, url, created_at)
VALUES (gen_random_uuid(), 'Loja não identificada', 'loja-nao-identificada', NULL, now())
ON CONFLICT (slug) DO NOTHING;
