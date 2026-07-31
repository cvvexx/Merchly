TRUNCATE TABLE cart_items;

ALTER TABLE cart_items
    ALTER COLUMN id DROP DEFAULT;

ALTER TABLE cart_items
    ALTER COLUMN id SET DATA TYPE UUID USING gen_random_uuid();

ALTER TABLE cart_items
    ALTER COLUMN id SET DEFAULT gen_random_uuid();