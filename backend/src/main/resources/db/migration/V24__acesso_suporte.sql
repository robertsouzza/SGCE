-- ============================================================
-- V24: break-glass dual-control do Super Admin (D-09)
-- ============================================================
-- solicitacoes_acesso_suporte: fluxo de solicitação/aprovação
-- acessos_suporte_log: eventos de acesso reais (uma linha por sessão
--   iniciada + uma linha por request feito com a sessão ativa).
--
-- Sem RLS aqui — só SUPER_ADMIN acessa via endpoint dedicado
-- (filtro na camada de aplicação, não no BD).
-- ============================================================

CREATE TABLE solicitacoes_acesso_suporte (
    id BIGSERIAL PRIMARY KEY,
    solicitante_id BIGINT NOT NULL REFERENCES usuarios(id),
    partido_alvo_id BIGINT NOT NULL REFERENCES partidos(id),
    motivo VARCHAR(500) NOT NULL,
    escopo VARCHAR(300) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    aprovador_id BIGINT REFERENCES usuarios(id),
    aprovada_em TIMESTAMPTZ,
    aprovacao_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    negada_em TIMESTAMPTZ,
    motivo_negacao VARCHAR(500),
    CONSTRAINT solicitacoes_status_valido CHECK (
        status IN ('PENDENTE','APROVADA','NEGADA','EXPIRADA','FINALIZADA')
    )
);

CREATE INDEX idx_solicitacoes_status ON solicitacoes_acesso_suporte (status, criada_em DESC);
CREATE INDEX idx_solicitacoes_solicitante ON solicitacoes_acesso_suporte (solicitante_id, criada_em DESC);

-- Permissão explícita para sgce_app (sem RLS — filtro na app):
GRANT SELECT, INSERT, UPDATE ON solicitacoes_acesso_suporte TO sgce_app;
GRANT USAGE, SELECT ON SEQUENCE solicitacoes_acesso_suporte_id_seq TO sgce_app;

-- ------------------------------------------------------------

CREATE TABLE acessos_suporte_log (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes_acesso_suporte(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    partido_id_acessado BIGINT NOT NULL REFERENCES partidos(id),
    escopo_acesso VARCHAR(300) NOT NULL,
    iniciado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em TIMESTAMPTZ NOT NULL,
    finalizado_em TIMESTAMPTZ,
    token_sessao VARCHAR(80) NOT NULL UNIQUE
);

CREATE INDEX idx_acessos_solicitacao ON acessos_suporte_log (solicitacao_id);
CREATE INDEX idx_acessos_token ON acessos_suporte_log (token_sessao);
CREATE INDEX idx_acessos_usuario ON acessos_suporte_log (usuario_id, iniciado_em DESC);

GRANT SELECT, INSERT, UPDATE ON acessos_suporte_log TO sgce_app;
GRANT USAGE, SELECT ON SEQUENCE acessos_suporte_log_id_seq TO sgce_app;
