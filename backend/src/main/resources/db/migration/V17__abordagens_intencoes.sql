-- ============================================================
-- V17: abordagens (visita/interação) + intenções de voto (N:N)
-- ============================================================
-- Uma abordagem pode registrar intenção para 1 ou vários candidatos
-- (RF-14 — cobre abordagem para 1 candidato ou para a chapa toda).
-- timestamp_local vem do dispositivo (relógio local, mesmo offline);
-- timestamp_sincronizacao é o now() do servidor no upsert.
-- ============================================================

CREATE TABLE abordagens (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    eleitor_id BIGINT NOT NULL REFERENCES eleitores(id),
    membro_id BIGINT NOT NULL REFERENCES usuarios(id),
    equipe_id BIGINT REFERENCES equipes(id),
    tipo_abordagem VARCHAR(20) NOT NULL,
    data_hora TIMESTAMPTZ NOT NULL,
    geolocalizacao_abordagem geometry(Point, 4326),
    timestamp_local TIMESTAMPTZ,
    timestamp_sincronizacao TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sincronizado BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT abordagens_tipo_valido CHECK (
        tipo_abordagem IN ('DOMICILIAR','PUBLICA')
    )
);

CREATE INDEX idx_abordagens_partido ON abordagens (partido_id, criado_em DESC);
CREATE INDEX idx_abordagens_eleitor ON abordagens (eleitor_id);
CREATE INDEX idx_abordagens_membro ON abordagens (membro_id, data_hora DESC);
CREATE INDEX idx_abordagens_geo ON abordagens USING GIST (geolocalizacao_abordagem);

ALTER TABLE abordagens ENABLE ROW LEVEL SECURITY;
ALTER TABLE abordagens FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON abordagens
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);

-- ------------------------------------------------------------

CREATE TABLE intencoes_voto (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    abordagem_id BIGINT NOT NULL REFERENCES abordagens(id) ON DELETE CASCADE,
    candidato_id BIGINT NOT NULL REFERENCES candidatos(id),
    intencao VARCHAR(20) NOT NULL,
    CONSTRAINT intencoes_valida CHECK (
        intencao IN ('FAVORAVEL','INDECISO','CONTRARIO','HOSTIL')
    ),
    CONSTRAINT intencoes_unica UNIQUE (abordagem_id, candidato_id)
);

CREATE INDEX idx_intencoes_partido ON intencoes_voto (partido_id);
CREATE INDEX idx_intencoes_candidato_intencao ON intencoes_voto (candidato_id, intencao);

ALTER TABLE intencoes_voto ENABLE ROW LEVEL SECURITY;
ALTER TABLE intencoes_voto FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON intencoes_voto
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
