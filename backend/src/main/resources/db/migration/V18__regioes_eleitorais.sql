-- ============================================================
-- V18: RegiaoEleitoral hierárquica (PAIS > ESTADO > MUNICIPIO > BAIRRO_ZONA)
-- ============================================================
-- Dados públicos (não sensíveis) — sem RLS. Serve para todos partidos.
-- Malha REAL do IBGE é importada via scripts/import-ibge.sh (skill 10,
-- decisão D-06). Aqui só o schema; o seed sintético vem na V19.
-- ============================================================

CREATE TABLE regioes_eleitorais (
    id BIGSERIAL PRIMARY KEY,
    nivel VARCHAR(20) NOT NULL,
    regiao_pai_id BIGINT REFERENCES regioes_eleitorais(id),
    codigo_ibge VARCHAR(30),
    nome_regiao VARCHAR(200) NOT NULL,
    geometria geometry(Polygon, 4326),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT regioes_nivel_valido CHECK (
        nivel IN ('PAIS','ESTADO','MUNICIPIO','BAIRRO_ZONA')
    )
);

CREATE INDEX idx_regioes_nivel ON regioes_eleitorais (nivel);
CREATE INDEX idx_regioes_pai ON regioes_eleitorais (regiao_pai_id);
CREATE INDEX idx_regioes_codigo ON regioes_eleitorais (codigo_ibge);
CREATE INDEX idx_regioes_geo ON regioes_eleitorais USING GIST (geometria);

-- Permissão explícita para sgce_app operar (RLS não se aplica: sem policy).
GRANT SELECT ON regioes_eleitorais TO sgce_app;
