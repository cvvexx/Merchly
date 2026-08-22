DROP INDEX IF EXISTS idx_reviews_product_id;

CREATE INDEX idx_reviews_product_id_created_at ON product_reviews (product_id, created_at DESC);
