-- ============================================================
-- V10: tabela partidos + RLS multi-tenant
-- ============================================================
-- FORCE ROW LEVEL SECURITY é necessário porque o usuário 'sgce'
-- da aplicação também é o owner das tabelas (por rodar as
-- migrations Flyway como esse usuário), e por padrão o owner
-- bypassa RLS. FORCE aplica RLS mesmo ao owner.
-- ============================================================

CREATE TABLE partidos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    sigla VARCHAR(20) NOT NULL UNIQUE,
    numero_partido INT NOT NULL UNIQUE,
    cnpj VARCHAR(20) NOT NULL UNIQUE,
    endereco_sede VARCHAR(500),
    dados_bancarios_conta_partidaria VARCHAR(500),
    email VARCHAR(200),
    telefone VARCHAR(50),
    plano_assinatura VARCHAR(50) NOT NULL DEFAULT 'FREE',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_partidos_sigla ON partidos (sigla);

ALTER TABLE partidos ENABLE ROW LEVEL SECURITY;
ALTER TABLE partidos FORCE ROW LEVEL SECURITY;

-- Policy: partidos são vistos apenas pelo próprio partido dono,
-- exceto quando app.current_partido_id está vazio (SUPER_ADMIN sem
-- sessão de suporte — vê a lista para operar métricas agregadas).
-- Uso de NULLIF para não estourar cast se o setting estiver vazio.
CREATE POLICY tenant_isolation ON partidos
    USING (
        NULLIF(current_setting('app.current_partido_id', TRUE), '') IS NULL
        OR id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT
    )
    WITH CHECK (TRUE);
