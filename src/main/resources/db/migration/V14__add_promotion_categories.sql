-- Additive many-to-many-like category collection using category names as the
-- existing stable identifier. The legacy promotion.category column is retained
-- during rollout so older API/UI versions remain compatible.
CREATE TABLE promotion_category (
    promotion_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_promotion_category
        PRIMARY KEY (promotion_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uq_promotion_category_name
        UNIQUE (promotion_id, category) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_promotion_category_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE
);

CREATE INDEX idx_promotion_category_name ON promotion_category(category);

INSERT INTO promotion_category (promotion_id, category, position)
SELECT id, TRIM(category), 0
FROM promotion
WHERE category IS NOT NULL AND LENGTH(TRIM(category)) > 0;
