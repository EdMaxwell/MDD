INSERT INTO users (id, email, username, password_hash, is_active, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'alice@mdd.local', 'Alice Martin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now()),
    ('10000000-0000-0000-0000-000000000002', 'bob@mdd.local', 'Bob Durand', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now()),
    ('10000000-0000-0000-0000-000000000003', 'charlie@mdd.local', 'Charlie Petit', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO topics (id, slug, name, description, is_active, created_at, updated_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'spring-boot', 'Spring Boot', 'Framework Java pour creer des APIs robustes et maintenables.', true, now(), now()),
    ('20000000-0000-0000-0000-000000000002', 'angular', 'Angular', 'Framework frontend TypeScript pour construire des applications web.', true, now(), now()),
    ('20000000-0000-0000-0000-000000000003', 'postgresql', 'PostgreSQL', 'Base de donnees relationnelle open source orientee fiabilite.', true, now(), now()),
    ('20000000-0000-0000-0000-000000000004', 'architecture', 'Architecture', 'Bonnes pratiques de conception et d organisation applicative.', true, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_topic_subscriptions (id, user_id, topic_id, created_at)
VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', now()),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', now()),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', now()),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO posts (id, author_id, topic_id, title, content, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Demarrer une API Spring Boot', 'Quelques reperes pour organiser controller, service et repository dans MDD.', now() - interval '3 days', now() - interval '3 days'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Structurer les routes Angular', 'Retour d experience sur une organisation feature-first pour une SPA.', now() - interval '2 days', now() - interval '2 days'),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'Pourquoi PostgreSQL en local', 'Docker Compose permet de garder une base reproductible pour toute l equipe.', now() - interval '1 day', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO comments (id, post_id, author_id, content, created_at, updated_at)
VALUES
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Merci, c est clair pour demarrer le backend.', now() - interval '2 days', now() - interval '2 days'),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'L organisation par feature colle bien au frontend MDD.', now() - interval '1 day', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;
