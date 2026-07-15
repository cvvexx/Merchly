CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE check ( length(trim(username)) > 0 ),
    password VARCHAR(255) NOT NULL check ( length(trim(password)) > 0 )
);

create table roles
(
    id   serial primary key,
    role varchar(60) unique not null check ( length(trim(role)) > 0 )
);

create table user_roles
(
    id      serial primary key,
    user_id int not null references users (id) on delete cascade,
    role_id int not null references roles (id) on delete cascade,
    constraint uk_user_role unique (user_id, role_id)
);