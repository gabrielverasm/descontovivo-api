CREATE TABLE promotion (
    id UUID PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    description TEXT NOT NULL,
    normalized_description TEXT NOT NULL,
    current_price NUMERIC(12,2) NOT NULL,
    original_price NUMERIC(12,2),
    coupon_code VARCHAR(100),
    image_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    availability VARCHAR(30) NOT NULL,
    store_id UUID NOT NULL REFERENCES store(id),
    created_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ
);

CREATE INDEX idx_promotion_status_created ON promotion(status, created_at DESC);
CREATE INDEX idx_promotion_store ON promotion(store_id);
CREATE INDEX idx_promotion_slug ON promotion(slug);
CREATE INDEX idx_promotion_normalized_url ON promotion(normalized_url);
CREATE INDEX idx_promotion_normalized_desc ON promotion(normalized_description);
CREATE UNIQUE INDEX uq_promotion_dedup ON promotion(normalized_url, normalized_description, created_date);
