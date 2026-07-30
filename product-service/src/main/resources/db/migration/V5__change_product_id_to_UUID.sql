ALTER TABLE products
    ALTER COLUMN id DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN id SET DATA TYPE UUID USING gen_random_uuid();

ALTER TABLE products
    ALTER COLUMN id SET DEFAULT gen_random_uuid();