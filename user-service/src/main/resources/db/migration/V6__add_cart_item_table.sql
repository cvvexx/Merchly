CREATE TABLE cart_items
(
    id         SERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id),
    product_id INT  NOT NULL,
    quantity   INT  NOT NULL DEFAULT 1 CHECK (quantity > 0),
    CONSTRAINT uk_user_product_cart UNIQUE (user_id, product_id)
);