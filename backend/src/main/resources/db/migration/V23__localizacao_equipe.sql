-- ============================================================
-- V23: histórico de localização de membros em campo (tempo real)
-- ============================================================
-- Fonte de verdade da localização "agora" é Redis (canal por partido).
-- Esta tabela guarda histórico opcional para replay/auditoria — utilidade
-- futura, não usada no fluxo síncrono.
-- ============================================================

CREATE TABLE localizacao_equipe_tempo_real (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    membro_id BIGINT NOT NULL REFERENCES usuarios(id),
    geolocalizacao geometry(Point, 4326) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status_conexao VARCHAR(30) NOT NULL,
    CONSTRAINT localizacao_status_valido CHECK (
        status_conexao IN ('ONLINE','OFFLINE_COLETANDO')
    )
);

CREATE INDEX idx_localizacao_partido_ts ON localizacao_equipe_tempo_real (partido_id, timestamp DESC);
CREATE INDEX idx_localizacao_membro_ts ON localizacao_equipe_tempo_real (membro_id, timestamp DESC);
CREATE INDEX idx_localizacao_geo ON localizacao_equipe_tempo_real USING GIST (geolocalizacao);

ALTER TABLE localizacao_equipe_tempo_real ENABLE ROW LEVEL SECURITY;
ALTER TABLE localizacao_equipe_tempo_real FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON localizacao_equipe_tempo_real
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
