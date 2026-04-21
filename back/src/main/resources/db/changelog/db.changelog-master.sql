--liquibase formatted sql

--changeset codex:2026-04-20-000-migrate-legacy-hibernate-schema dbms:postgresql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:bigint SELECT COALESCE((SELECT data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'id'), 'none')
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE IF EXISTS subscriptions DROP CONSTRAINT IF EXISTS fkfuag8et2vdg3ds9h8trqx2ldq;
ALTER TABLE IF EXISTS subscriptions DROP CONSTRAINT IF EXISTS fkhro52ohfqfbay9774bev0qinr;
ALTER TABLE IF EXISTS users RENAME TO users_legacy;
ALTER TABLE IF EXISTS topics RENAME TO topics_legacy;
ALTER TABLE IF EXISTS subscriptions RENAME TO subscriptions_legacy;

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(150) NOT NULL,
    username varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
);

CREATE TABLE topics (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_topic_subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE posts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    title varchar(180) NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    device_name varchar(100),
    user_agent varchar(255),
    ip_address inet
);

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD CONSTRAINT uk_users_username UNIQUE (username);

ALTER TABLE topics
    ADD CONSTRAINT uk_topics_slug UNIQUE (slug),
    ADD CONSTRAINT uk_topics_name UNIQUE (name);

ALTER TABLE user_topic_subscriptions
    ADD CONSTRAINT fk_user_topic_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_user_topic_subscriptions_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT,
    ADD CONSTRAINT uk_user_topic_subscriptions_user_topic UNIQUE (user_id, topic_id);

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_posts_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT;

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash);

CREATE INDEX idx_user_topic_subscriptions_user_id ON user_topic_subscriptions (user_id);
CREATE INDEX idx_user_topic_subscriptions_topic_id ON user_topic_subscriptions (topic_id);
CREATE INDEX idx_posts_author_id ON posts (author_id);
CREATE INDEX idx_posts_topic_id ON posts (topic_id);
CREATE INDEX idx_posts_created_at ON posts (created_at);
CREATE INDEX idx_posts_topic_created_at ON posts (topic_id, created_at);
CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

INSERT INTO users (id, email, username, password_hash, is_active, created_at, updated_at)
SELECT ('00000000-0000-0000-0001-' || lpad(id::text, 12, '0'))::uuid,
       email,
       name,
       password,
       true,
       now(),
       now()
FROM users_legacy
ON CONFLICT (id) DO NOTHING;

INSERT INTO topics (id, slug, name, description, is_active, created_at, updated_at)
SELECT ('00000000-0000-0000-0002-' || lpad(id::text, 12, '0'))::uuid,
       regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g'),
       name,
       description,
       true,
       now(),
       now()
FROM topics_legacy
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_topic_subscriptions (id, user_id, topic_id, created_at)
SELECT gen_random_uuid(),
       ('00000000-0000-0000-0001-' || lpad(user_id::text, 12, '0'))::uuid,
       ('00000000-0000-0000-0002-' || lpad(topic_id::text, 12, '0'))::uuid,
       now()
FROM subscriptions_legacy
ON CONFLICT (user_id, topic_id) DO NOTHING;
--rollback DROP TABLE IF EXISTS refresh_tokens;
--rollback DROP TABLE IF EXISTS comments;
--rollback DROP TABLE IF EXISTS posts;
--rollback DROP TABLE IF EXISTS user_topic_subscriptions;
--rollback DROP TABLE IF EXISTS topics;
--rollback DROP TABLE IF EXISTS users;
--rollback ALTER TABLE IF EXISTS users_legacy RENAME TO users;
--rollback ALTER TABLE IF EXISTS topics_legacy RENAME TO topics;
--rollback ALTER TABLE IF EXISTS subscriptions_legacy RENAME TO subscriptions;

--changeset codex:2026-04-20-001-initial-schema dbms:postgresql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users'
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(150) NOT NULL,
    username varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
);

CREATE TABLE topics (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_topic_subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE posts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    title varchar(180) NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    device_name varchar(100),
    user_agent varchar(255),
    ip_address inet
);

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD CONSTRAINT uk_users_username UNIQUE (username);

ALTER TABLE topics
    ADD CONSTRAINT uk_topics_slug UNIQUE (slug),
    ADD CONSTRAINT uk_topics_name UNIQUE (name);

ALTER TABLE user_topic_subscriptions
    ADD CONSTRAINT fk_user_topic_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_user_topic_subscriptions_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT,
    ADD CONSTRAINT uk_user_topic_subscriptions_user_topic UNIQUE (user_id, topic_id);

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_posts_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE RESTRICT;

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash);

CREATE INDEX idx_user_topic_subscriptions_user_id ON user_topic_subscriptions (user_id);
CREATE INDEX idx_user_topic_subscriptions_topic_id ON user_topic_subscriptions (topic_id);
CREATE INDEX idx_posts_author_id ON posts (author_id);
CREATE INDEX idx_posts_topic_id ON posts (topic_id);
CREATE INDEX idx_posts_created_at ON posts (created_at);
CREATE INDEX idx_posts_topic_created_at ON posts (topic_id, created_at);
CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
--rollback DROP TABLE IF EXISTS refresh_tokens;
--rollback DROP TABLE IF EXISTS comments;
--rollback DROP TABLE IF EXISTS posts;
--rollback DROP TABLE IF EXISTS user_topic_subscriptions;
--rollback DROP TABLE IF EXISTS topics;
--rollback DROP TABLE IF EXISTS users;
