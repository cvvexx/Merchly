CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_title_trgm
    ON products USING gin (upper(title) gin_trgm_ops)
    WHERE is_available = true;
