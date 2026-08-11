-- ============================================================
-- V13: recursos financeiros (fundo eleitoral, fundo partidário, doação)
-- ============================================================

CREATE TABLE recursos (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    candidato_id BIGINT NOT NULL REFERENCES candidatos(id),
    tipo_recurso VARCHAR(30) NOT NULL,
    valor NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    data_repasse DATE NOT NULL,
    origem VARCHAR(300),
    numero_documento VARCHAR(100),
    comprovante_url VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT recursos_tipo_valido CHECK (
        tipo_recurso IN ('FUNDO_ELEITORAL','FUNDO_PARTIDARIO','DOACAO')
    )
);

CREATE INDEX idx_recursos_partido ON recursos (partido_id, criado_em DESC);
CREATE INDEX idx_recursos_candidato ON recursos (candidato_id);

ALTER TABLE recursos ENABLE ROW LEVEL SECURITY;
ALTER TABLE recursos FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON recursos
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
