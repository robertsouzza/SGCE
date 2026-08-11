-- ============================================================
-- V14: despesas com fluxo de aprovação PENDENTE→APROVADO/REJEITADO
-- ============================================================

CREATE TABLE despesas (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    candidato_id BIGINT NOT NULL REFERENCES candidatos(id),
    categoria VARCHAR(30) NOT NULL,
    subcategoria_tse VARCHAR(100),
    valor NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    data DATE NOT NULL,
    descricao VARCHAR(500),
    lancado_por BIGINT NOT NULL REFERENCES usuarios(id),
    comprovante_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    aprovado_por BIGINT REFERENCES usuarios(id),
    aprovado_em TIMESTAMPTZ,
    motivo_rejeicao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT despesas_categoria_valida CHECK (
        categoria IN ('PESSOAL','ALIMENTACAO','TRANSPORTE','MATERIAL_GRAFICO','OUTROS')
    ),
    CONSTRAINT despesas_status_valido CHECK (
        status IN ('PENDENTE','APROVADO','REJEITADO')
    )
);

CREATE INDEX idx_despesas_partido_status ON despesas (partido_id, status, criado_em DESC);
CREATE INDEX idx_despesas_candidato ON despesas (candidato_id);

ALTER TABLE despesas ENABLE ROW LEVEL SECURITY;
ALTER TABLE despesas FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON despesas
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
