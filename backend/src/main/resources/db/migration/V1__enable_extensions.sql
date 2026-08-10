-- ============================================================
-- V1: extensões e convenções globais
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================
-- Convenção de multi-tenancy (Row-Level Security)
-- ============================================================
-- Cada requisição de um usuário autenticado faz, dentro de uma
-- transação:
--
--   SET LOCAL app.current_partido_id = '<id do partido>';
--
-- SET LOCAL vive apenas até COMMIT/ROLLBACK, então quando a
-- conexão volta ao pool o valor morre — não vaza para o próximo
-- request. As RLS policies das tabelas de negócio lêem via:
--
--   current_setting('app.current_partido_id', TRUE)::BIGINT
--
-- O segundo argumento TRUE evita erro quando o parâmetro está
-- vazio (caso do SUPER_ADMIN_PLATAFORMA sem sessão de suporte).
-- ============================================================
