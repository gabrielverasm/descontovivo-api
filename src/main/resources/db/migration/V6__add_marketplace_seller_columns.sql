-- Marketplace and seller trust columns
ALTER TABLE promotion ADD COLUMN marketplace VARCHAR(50);
ALTER TABLE promotion ADD COLUMN seller_name VARCHAR(100);
ALTER TABLE promotion ADD COLUMN sold_by VARCHAR(100);
ALTER TABLE promotion ADD COLUMN delivered_by VARCHAR(100);
ALTER TABLE promotion ADD COLUMN category VARCHAR(50);
