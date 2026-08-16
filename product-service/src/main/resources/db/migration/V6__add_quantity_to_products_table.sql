ALTER TABLE products
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 check ( quantity >= 0 );