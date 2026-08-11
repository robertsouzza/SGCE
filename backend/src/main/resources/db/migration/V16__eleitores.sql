-- ============================================================
-- V16: eleitores com geolocalização PostGIS
-- ============================================================
-- titulo_eleitor é UNIQUE por partido (chave natural do upsert offline).
-- Anonimização (skill 05, D-02) apaga PII mas mantém id, partido_id
-- e um hash do título (para dedupe de re-cadastro futuro).
-- ============================================================

CREATE TABLE eleitores (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    nome_completo VARCHAR(200) NOT NULL,
    endereco VARCHAR(500),
    geolocalizacao geometry(Point, 4326),
    telefone_whatsapp VARCHAR(50),
    titulo_eleitor VARCHAR(20),
    titulo_eleitor_hash VARCHAR(64),
    zona_eleitoral VARCHAR(20),
    secao_eleitoral VARCHAR(20),
    observacoes VARCHAR(1000),
    anonimizado BOOLEAN NOT NULL DEFAULT FALSE,
    anonimizado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT eleitores_titulo_unico_por_partido UNIQUE (partido_id, titulo_eleitor)
);

CREATE INDEX idx_eleitores_partido ON eleitores (partido_id, criado_em DESC);
CREATE INDEX idx_eleitores_geo ON eleitores USING GIST (geolocalizacao);
CREATE INDEX idx_eleitores_titulo_hash ON eleitores (partido_id, titulo_eleitor_hash);

ALTER TABLE eleitores ENABLE ROW LEVEL SECURITY;
ALTER TABLE eleitores FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON eleitores
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
