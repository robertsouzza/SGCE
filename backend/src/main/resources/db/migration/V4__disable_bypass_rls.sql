-- ============================================================
-- V4: cria usuário 'sgce_app' sem SUPERUSER para runtime da app
-- ============================================================
-- SUPERUSER e BYPASSRLS SEMPRE ignoram RLS, mesmo com FORCE ROW
-- LEVEL SECURITY. Como o POSTGRES_USER do image oficial vira
-- SUPERUSER, precisamos de outro role para o backend usar em
-- runtime — sem esse privilégio, RLS não protege multi-tenant.
--
-- - sgce_app: LOGIN, NOSUPERUSER, NOBYPASSRLS. É quem o backend
--   usa em runtime (application.yml → spring.datasource.username).
-- - sgce: continua SUPERUSER, roda apenas migrations Flyway (via
--   spring.flyway.user).
-- ============================================================

-- Cria o usuário se ainda não existir.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sgce_app') THEN
        CREATE ROLE sgce_app LOGIN PASSWORD 'sgce_app_dev'
            NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END$$;

-- Permissões nas tabelas existentes (TRUNCATE ignora RLS — útil em cleanup
-- de testes de integração; em prod, uso normal é via SELECT/INSERT/UPDATE/DELETE
-- que passam pela RLS):
GRANT USAGE ON SCHEMA public TO sgce_app;
GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public TO sgce_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sgce_app;

-- E também para as tabelas que virão nas próximas migrations:
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON TABLES TO sgce_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO sgce_app;
