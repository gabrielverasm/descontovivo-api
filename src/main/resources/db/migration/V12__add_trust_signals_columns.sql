-- Add trust signals columns for all marketplaces
-- All columns are nullable to maintain backward compatibility
-- Supports Amazon, Mercado Livre, Magalu, Shopee, and AliExpress

-- Sales count (e.g., 5334)
ALTER TABLE promotion ADD COLUMN sales_count INTEGER;

-- Product rating (e.g., 4.8)
ALTER TABLE promotion ADD COLUMN product_rating DECIMAL(3,1);

-- Seller rating (e.g., 4.9)
ALTER TABLE promotion ADD COLUMN seller_rating DECIMAL(3,1);

-- Official store flag
ALTER TABLE promotion ADD COLUMN official_store BOOLEAN DEFAULT false;

-- Trust signals as JSON array for selected/derived signals
-- Contains platform-specific trust signals (e.g., AMAZON_PRIME, SHOPEE_GUARANTEE, etc.)
-- Armazena JSON serializado como texto para evitar complexidade com JSONB/Hibernate no MVP
-- A API converte List<String> <-> JSON string via TrustSignalsHelper
ALTER TABLE promotion ADD COLUMN trust_signals TEXT;