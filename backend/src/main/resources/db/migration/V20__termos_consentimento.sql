-- ============================================================
-- V20: TermoConsentimento (LGPD) — versionado por partido
-- ============================================================
-- Cada partido publica versões do termo; ao capturar um consentimento
-- guardamos qual versão o eleitor concordou (auditoria/prova).
-- ============================================================

CREATE TABLE termos_consentimento (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    versao INT NOT NULL,
    texto TEXT NOT NULL,
    vigente_a_partir TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vigente_ate TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT termos_versao_unica_por_partido UNIQUE (partido_id, versao)
);

CREATE INDEX idx_termos_partido_vigencia ON termos_consentimento (partido_id, vigente_a_partir DESC);

ALTER TABLE termos_consentimento ENABLE ROW LEVEL SECURITY;
ALTER TABLE termos_consentimento FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON termos_consentimento
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
