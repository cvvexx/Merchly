create table product
(
    id          serial primary key not null,
    title       varchar(50)        not null check ( length(trim(title)) > 0),
    description varchar(1000)
)

