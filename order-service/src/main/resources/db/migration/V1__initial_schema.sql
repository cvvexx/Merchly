CREATE TABLE orders
(
    id               UUID PRIMARY KEY,
    user_id          UUID                     NOT NULL,
    status           VARCHAR(50)              NOT NULL DEFAULT 'CREATED',
    total_amount     DECIMAL(10, 2)           NOT NULL,
    delivery_address VARCHAR(1000)            NOT NULL,
    comment          VARCHAR(2000),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_user_id ON orders (user_id);

CREATE TABLE order_item
(
    id         UUID PRIMARY KEY,
    order_id   UUID           NOT NULL,
    product_id UUID           NOT NULL,
    price      DECIMAL(10, 2) NOT NULL,
    quantity   INT            NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_product_id ON order_item (product_id);