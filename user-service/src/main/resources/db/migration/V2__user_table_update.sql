ALTER TABLE users
    ADD COLUMN email      VARCHAR(255) UNIQUE,
    ADD COLUMN gender     VARCHAR(10),
    ADD COLUMN birth_date DATE CHECK (birth_date <= CURRENT_DATE - INTERVAL '14 years');

ALTER TABLE users
    ADD CONSTRAINT check_users_password check ( length(trim(users.password)) >= 8 and
                                                length(trim(users.password)) <= 255);

UPDATE users
SET email      = COALESCE(email, 'user_' || id || '@test.com'),
    gender     = COALESCE(gender, 'UNKNOWN'),
    birth_date = COALESCE(birth_date, '1970-01-01')
WHERE email IS NULL
   OR gender IS NULL
   OR birth_date IS NULL;

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN gender SET NOT NULL,
    ALTER COLUMN birth_date SET NOT NULL;