-- =============================================================================
-- Демонстрационные пользователи Merchly.
--
-- Подключается только там, где явно указан путь classpath:db/seed
-- (см. SPRING_FLYWAY_LOCATIONS в docker-compose.prod.yml). В dev и в тестах
-- этот файл не выполняется.
--
-- Идентификаторы фиксированные: те же UUID прописаны в .keycloak/realm-export.json,
-- в products.created_by и в product_reviews.user_id. Менять их можно только
-- одновременно во всех четырёх местах.
--
--   a0000000-…-000001  ink_studio    автор, роль ADMIN
--   a0000000-…-000002  tirazh_lab    автор
--   a0000000-…-000003  okhta_press   автор
--   a0000000-…-000004  kate_v        покупатель
--   a0000000-…-000005  roman_d       покупатель
--   a0000000-…-000006  nastya_m      покупатель
--
-- Пароль у всех: demo1234
-- =============================================================================

INSERT INTO users (id, username, email, gender, birth_date, avatar_file_name)
VALUES ('a0000000-0000-0000-0000-000000000001', 'ink_studio', 'ink.studio@merchly.dev', 'F', '1993-03-14',
        'ink-studio.png'),
       ('a0000000-0000-0000-0000-000000000002', 'tirazh_lab', 'tirazh.lab@merchly.dev', 'M', '1990-07-02',
        'tirazh-lab.png'),
       ('a0000000-0000-0000-0000-000000000003', 'okhta_press', 'okhta.press@merchly.dev', 'M', '1995-11-20',
        'okhta-press.png'),
       ('a0000000-0000-0000-0000-000000000004', 'kate_v', 'kate.v@merchly.dev', 'F', '1998-05-09',
        'kate-v.png'),
       ('a0000000-0000-0000-0000-000000000005', 'roman_d', 'roman.d@merchly.dev', 'M', '1996-01-27',
        'roman-d.png'),
       ('a0000000-0000-0000-0000-000000000006', 'nastya_m', 'nastya.m@merchly.dev', 'F', '2000-09-03',
        'nastya-m.png')
ON CONFLICT DO NOTHING;

-- Роли берём из таблицы roles по имени: её наполняет V9__basic_user_roles_init.sql
INSERT INTO user_roles (user_id, role_id)
SELECT seed.user_id::uuid, roles.id
FROM (VALUES ('a0000000-0000-0000-0000-000000000001', 'USER'),
             ('a0000000-0000-0000-0000-000000000001', 'ADMIN'),
             ('a0000000-0000-0000-0000-000000000002', 'USER'),
             ('a0000000-0000-0000-0000-000000000003', 'USER'),
             ('a0000000-0000-0000-0000-000000000004', 'USER'),
             ('a0000000-0000-0000-0000-000000000005', 'USER'),
             ('a0000000-0000-0000-0000-000000000006', 'USER')) AS seed(user_id, role)
         JOIN roles ON roles.role = seed.role
ON CONFLICT DO NOTHING;
