DROP INDEX IF EXISTS idx_order_user_id;

CREATE INDEX idx_order_user_id_created_at ON orders (user_id, created_at DESC);
