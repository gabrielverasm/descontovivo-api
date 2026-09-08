-- Approximate engagement counter for promotion detail page views.
-- Not deduplicated by user/IP and not a precise audit metric -- incremented on
-- every non-bot GET /promotions/{slug} request (see BotUserAgentDetector).
ALTER TABLE promotion ADD COLUMN view_count INT NOT NULL DEFAULT 0;
