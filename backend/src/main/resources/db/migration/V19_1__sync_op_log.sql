-- ============================================================
-- V20: log de idempotência para SincronizacaoController
-- ============================================================
-- Cada operação recebida do app offline carrega um client_op_id UUID.
-- Se já foi processada nos últimos 30 dias, ignoramos silenciosamente
-- e retornamos o server_id que foi criado antes (D-04, RF-17).
-- ============================================================

CREATE TABLE sync_op_log (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    client_op_id UUID NOT NULL UNIQUE,
    entidade VARCHAR(50) NOT NULL,
    server_entity_id BIGINT,
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_op_log_criado_em ON sync_op_log (criado_em);
CREATE INDEX idx_sync_op_log_partido ON sync_op_log (partido_id);

ALTER TABLE sync_op_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_op_log FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON sync_op_log
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
