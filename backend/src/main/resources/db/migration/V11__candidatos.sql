-- ============================================================
-- V11: tabela candidatos + RLS por partido_id
-- ============================================================

CREATE TABLE candidatos (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    nome_completo VARCHAR(200) NOT NULL,
    titulo_eleitor VARCHAR(20) NOT NULL,
    numero_candidato INT NOT NULL,
    cargo VARCHAR(30) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    municipio VARCHAR(150),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT candidatos_titulo_unico_por_partido UNIQUE (partido_id, titulo_eleitor),
    CONSTRAINT candidatos_cargo_valido CHECK (
        cargo IN ('PRESIDENTE','SENADOR','DEPUTADO_FEDERAL','DEPUTADO_ESTADUAL','PREFEITO','VEREADOR')
    ),
    -- Regra também validada no domínio, mas defesa em profundidade:
    CONSTRAINT candidatos_municipio_obrigatorio CHECK (
        (cargo NOT IN ('PREFEITO','VEREADOR')) OR (municipio IS NOT NULL AND length(trim(municipio)) > 0)
    )
);

CREATE INDEX idx_candidatos_partido ON candidatos (partido_id, criado_em DESC);
CREATE INDEX idx_candidatos_cargo ON candidatos (cargo);

ALTER TABLE candidatos ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidatos FORCE ROW LEVEL SECURITY;

-- SUPER_ADMIN sem sessão de suporte NÃO vê candidato individual (só métricas
-- agregadas — para conteúdo sensível existe o break-glass). Por isso o cast
-- via NULLIF: se o setting está vazio, vira NULL, comparação com NULL é FALSE
-- e a policy filtra tudo. Só ADMIN do partido (setting = seu id) vê.
CREATE POLICY tenant_isolation ON candidatos
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
