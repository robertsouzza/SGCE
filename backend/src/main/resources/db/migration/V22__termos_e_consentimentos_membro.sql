-- ============================================================
-- V22: Termo + Consentimento do MEMBRO/voluntário (D-10)
-- ============================================================
-- LGPD art. 8º §4º exige consentimento específico por finalidade.
-- Rastreamento em tempo real do voluntário em campo é finalidade
-- distinta de "usar o app" → termo próprio + consentimento próprio,
-- revogável e verificável antes de cada heartbeat de localização
-- (a validação real do rastreamento fica na skill 06).
-- ============================================================

CREATE TABLE termos_consentimento_membro (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    versao INT NOT NULL,
    texto TEXT NOT NULL,
    vigente_a_partir TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vigente_ate TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT termos_membro_versao_unica_por_partido UNIQUE (partido_id, versao)
);

ALTER TABLE termos_consentimento_membro ENABLE ROW LEVEL SECURITY;
ALTER TABLE termos_consentimento_membro FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON termos_consentimento_membro
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);

-- ------------------------------------------------------------

CREATE TABLE consentimentos_membro (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    termo_versao_id BIGINT NOT NULL REFERENCES termos_consentimento_membro(id),

    consentimento_rastreamento BOOLEAN NOT NULL,
    consentimento_rastreamento_em TIMESTAMPTZ,
    consentimento_rastreamento_revogado BOOLEAN NOT NULL DEFAULT FALSE,
    consentimento_rastreamento_revogado_em TIMESTAMPTZ,

    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT consentimentos_membro_unico_ativo UNIQUE (usuario_id, termo_versao_id)
);

CREATE INDEX idx_consentimentos_membro_usuario ON consentimentos_membro (usuario_id);

ALTER TABLE consentimentos_membro ENABLE ROW LEVEL SECURITY;
ALTER TABLE consentimentos_membro FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON consentimentos_membro
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
