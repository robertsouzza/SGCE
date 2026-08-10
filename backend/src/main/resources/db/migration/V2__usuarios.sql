-- ============================================================
-- V2: tabela de usuários
-- ============================================================
-- partido_id é NULL apenas para perfil SUPER_ADMIN_PLATAFORMA,
-- que atua acima de qualquer partido (dashboard de métricas
-- operacionais agregadas + break-glass dual-control auditado).
-- Seed de 1 SUPER_ADMIN sintético é criado por DataInitializer
-- no bootstrap da aplicação (apenas em profiles dev/docker).
-- ============================================================

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT,
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    telefone VARCHAR(50),
    perfil VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_partido ON usuarios (partido_id);
CREATE INDEX idx_usuarios_perfil ON usuarios (perfil);

COMMENT ON COLUMN usuarios.partido_id IS 'NULL apenas para SUPER_ADMIN_PLATAFORMA';
COMMENT ON COLUMN usuarios.perfil IS 'SUPER_ADMIN_PLATAFORMA, ADMIN, CANDIDATO, GERENTE_FINANCEIRO, SECRETARIO, LIDER_EQUIPE, MEMBRO_EQUIPE';
