-- ============================================================
-- V15: pagamento a membro de equipe (especialização de despesa PESSOAL)
-- ============================================================

CREATE TABLE pagamentos_equipe (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    despesa_id BIGINT NOT NULL UNIQUE REFERENCES despesas(id),
    membro_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo_pagamento VARCHAR(30) NOT NULL,
    quantidade INT NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(15,2) NOT NULL CHECK (valor_unitario > 0),
    periodo_referencia VARCHAR(30),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pagamentos_tipo_valido CHECK (
        tipo_pagamento IN ('DIARIA','SALARIO','POR_ABORDAGEM','POR_VISITA')
    )
);

CREATE INDEX idx_pagamentos_partido ON pagamentos_equipe (partido_id, criado_em DESC);
CREATE INDEX idx_pagamentos_membro ON pagamentos_equipe (membro_id);

ALTER TABLE pagamentos_equipe ENABLE ROW LEVEL SECURITY;
ALTER TABLE pagamentos_equipe FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON pagamentos_equipe
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
