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
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'Pourquoi PostgreSQL en local', 'Docker Compose permet de garder une base reproductible pour toute l equipe.', now() - interval '1 day', now() - interval '1 day'),
    ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'DTO ou entite JPA dans une API', 'Un DTO explicite evite de faire fuiter le modele de persistence et stabilise le contrat public.', now() - interval '11 hours', now() - interval '11 hours'),
    ('40000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'Signals Angular pour les etats UI', 'Les signals rendent les etats de chargement, erreur et donnees plus lisibles dans les composants standalone.', now() - interval '10 hours', now() - interval '10 hours'),
    ('40000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Limiter les volumes avec Pageable', 'Paginer cote serveur permet de garder une interface rapide et une API previsibile quand le nombre d articles augmente.', now() - interval '9 hours', now() - interval '9 hours'),
    ('40000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Composer une page Angular maintenable', 'Un composant page orchestre les appels et delegue l affichage repetable a des composants reutilisables.', now() - interval '8 hours', now() - interval '8 hours'),
    ('40000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'EntityGraph contre le probleme N plus un', 'Charger auteur et theme dans la meme requete rend le mapping du feed plus efficace et plus previsible.', now() - interval '7 hours', now() - interval '7 hours'),
    ('40000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Un paginator simple pour le feed', 'Une pagination explicite aide a tester le comportement attendu et garde six cartes maximum par page.', now() - interval '6 hours', now() - interval '6 hours'),
    ('40000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Index utiles pour les lectures recentes', 'Un index sur les dates de creation facilite les tris chronologiques sur les listes d articles.', now() - interval '5 hours', now() - interval '5 hours'),
    ('40000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', 'Decouper sans sur-abstraire', 'Un bon decoupage reduit la duplication sans cacher la logique metier derriere trop de couches.', now() - interval '4 hours', now() - interval '4 hours'),
    ('40000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Valider les entrees au bord de l API', 'La validation des payloads dans les controllers garde les services concentres sur les regles metier.', now() - interval '3 hours', now() - interval '3 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO comments (id, post_id, author_id, content, created_at, updated_at)
VALUES
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Merci, c est clair pour demarrer le backend.', now() - interval '2 days', now() - interval '2 days'),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'L organisation par feature colle bien au frontend MDD.', now() - interval '1 day', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;
