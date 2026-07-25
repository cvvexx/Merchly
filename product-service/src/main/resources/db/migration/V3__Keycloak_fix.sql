ALTER TABLE product RENAME TO products;

ALTER TABLE products
    ALTER COLUMN created_by DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN created_by TYPE UUID
        USING '65fc824b-5d21-4ab1-9b57-f4adf19bb60e'::uuid;

ALTER TABLE products
    ALTER COLUMN created_by SET NOT NULL;