-- ============================================================
-- V3: log de auditoria
-- ============================================================
-- Preenchido automaticamente por AuditoriaAspect (AOP @Around
-- em métodos anotados com @Auditavel). Guarda snapshot antes/
-- depois do payload da operação em jsonb para reconstituição.
-- ATENÇÃO: pode conter PII (nome/endereço/telefone de eleitor).
-- Acesso à tabela deve ser restrito ao perfil AUDITOR (a ser
-- criado em skill futura); por ora, somente uso interno via
-- app + acesso operacional via psql restrito.
-- ============================================================

CREATE TABLE logs_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(100) NOT NULL,
    entidade_id VARCHAR(100),
    dados_antes JSONB,
    dados_depois JSONB,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip VARCHAR(45)
);

CREATE INDEX idx_logs_auditoria_usuario ON logs_auditoria (usuario_id);
CREATE INDEX idx_logs_auditoria_entidade ON logs_auditoria (entidade, entidade_id);
CREATE INDEX idx_logs_auditoria_timestamp ON logs_auditoria (timestamp DESC);
