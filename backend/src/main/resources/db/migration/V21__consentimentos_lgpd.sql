-- ============================================================
-- V21: ConsentimentoLGPD — duas flags independentes (D-01, D-02)
-- ============================================================
-- consentimento_dados: autoriza tratamento dos dados/intenção de voto.
-- consentimento_whatsapp_marketing: autoriza recebimento de mensagens.
-- Ambos revogáveis independentemente (LGPD art. 8º §4º).
-- Revogar consentimento_dados dispara anonimização do Eleitor (D-02).
-- ============================================================

CREATE TABLE consentimentos_lgpd (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    eleitor_id BIGINT NOT NULL REFERENCES eleitores(id),
    abordagem_id BIGINT REFERENCES abordagens(id),
    termo_versao_id BIGINT NOT NULL REFERENCES termos_consentimento(id),
    metodo_captura VARCHAR(30) NOT NULL,
    assinatura_arquivo_url VARCHAR(500),
    membro_captura_id BIGINT NOT NULL REFERENCES usuarios(id),
    geolocalizacao geometry(Point, 4326),
    timestamp_local TIMESTAMPTZ,
    timestamp_sincronizacao TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    contato_salvo_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    cod VARCHAR(30),

    -- consentimento_dados
    consentimento_dados BOOLEAN NOT NULL,
    consentimento_dados_em TIMESTAMPTZ,
    consentimento_dados_revogado BOOLEAN NOT NULL DEFAULT FALSE,
    consentimento_dados_revogado_em TIMESTAMPTZ,

    -- consentimento_whatsapp_marketing
    consentimento_whatsapp_marketing BOOLEAN NOT NULL,
    consentimento_whatsapp_marketing_em TIMESTAMPTZ,
    consentimento_whatsapp_marketing_revogado BOOLEAN NOT NULL DEFAULT FALSE,
    consentimento_whatsapp_marketing_revogado_em TIMESTAMPTZ,

    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT consentimentos_metodo_valido CHECK (
        metodo_captura IN ('ASSINATURA_TELA','QRCODE_WHATSAPP')
    )
);

CREATE INDEX idx_consentimentos_eleitor ON consentimentos_lgpd (eleitor_id);
CREATE INDEX idx_consentimentos_partido ON consentimentos_lgpd (partido_id, criado_em DESC);
CREATE INDEX idx_consentimentos_cod ON consentimentos_lgpd (cod) WHERE cod IS NOT NULL;

ALTER TABLE consentimentos_lgpd ENABLE ROW LEVEL SECURITY;
ALTER TABLE consentimentos_lgpd FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON consentimentos_lgpd
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
