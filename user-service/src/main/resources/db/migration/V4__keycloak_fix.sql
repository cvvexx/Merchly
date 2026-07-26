ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS user_roles_user_id_fkey;

ALTER TABLE users
    ALTER COLUMN id TYPE UUID USING id::UUID;

ALTER TABLE user_roles
    ALTER COLUMN user_id TYPE UUID USING user_id::UUID;

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;