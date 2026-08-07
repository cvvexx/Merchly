CREATE TABLE product_reviews
(
    id         UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    rating     INT  NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment    VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_product_review UNIQUE (product_id, user_id)
);

CREATE INDEX idx_reviews_product_id ON product_reviews (product_id);
CREATE INDEX idx_reviews_user_id ON product_reviews (user_id);