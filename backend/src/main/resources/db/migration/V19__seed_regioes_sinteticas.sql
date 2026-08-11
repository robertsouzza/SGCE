-- ============================================================
-- V19: seed sintético de regiões (dev/teste)
-- ============================================================
-- DADOS SINTÉTICOS. Polígonos são bounding boxes arbitrários só para
-- o mapa renderizar em dev. NUNCA usar em produção — a malha real do
-- IBGE deve ser importada via scripts/import-ibge.sh (skill 10).
--
-- Estrutura: 1 país (Brasil-FAKE) → 3 estados fake (SP-FAKE, RJ-FAKE,
-- MG-FAKE) → alguns municípios por estado → alguns bairros/zonas por
-- município. Total: ~40 registros.
-- ============================================================

-- País
INSERT INTO regioes_eleitorais (nivel, nome_regiao, codigo_ibge, geometria) VALUES
('PAIS', 'Brasil-FAKE', 'BR-FAKE',
    ST_GeomFromText('POLYGON((-74 -34, -34 -34, -34 6, -74 6, -74 -34))', 4326));

-- Estados (pai = país id 1)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('ESTADO', 1, 'São Paulo-FAKE', 'SP-FAKE',
    ST_GeomFromText('POLYGON((-53 -25, -44 -25, -44 -19, -53 -19, -53 -25))', 4326)),
('ESTADO', 1, 'Rio de Janeiro-FAKE', 'RJ-FAKE',
    ST_GeomFromText('POLYGON((-45 -23.5, -41 -23.5, -41 -20.5, -45 -20.5, -45 -23.5))', 4326)),
('ESTADO', 1, 'Minas Gerais-FAKE', 'MG-FAKE',
    ST_GeomFromText('POLYGON((-51 -23, -40 -23, -40 -14, -51 -14, -51 -23))', 4326));

-- Municípios SP (pai = 2)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('MUNICIPIO', 2, 'São Paulo-FAKE', 'SP-3550308-FAKE',
    ST_GeomFromText('POLYGON((-46.83 -23.79, -46.36 -23.79, -46.36 -23.36, -46.83 -23.36, -46.83 -23.79))', 4326)),
('MUNICIPIO', 2, 'Campinas-FAKE', 'SP-3509502-FAKE',
    ST_GeomFromText('POLYGON((-47.20 -23.05, -46.85 -23.05, -46.85 -22.75, -47.20 -22.75, -47.20 -23.05))', 4326)),
('MUNICIPIO', 2, 'Santos-FAKE', 'SP-3548500-FAKE',
    ST_GeomFromText('POLYGON((-46.45 -24.05, -46.25 -24.05, -46.25 -23.85, -46.45 -23.85, -46.45 -24.05))', 4326)),
('MUNICIPIO', 2, 'Ribeirão Preto-FAKE', 'SP-3543402-FAKE',
    ST_GeomFromText('POLYGON((-47.90 -21.30, -47.65 -21.30, -47.65 -21.05, -47.90 -21.05, -47.90 -21.30))', 4326));

-- Municípios RJ (pai = 3)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('MUNICIPIO', 3, 'Rio de Janeiro-FAKE', 'RJ-3304557-FAKE',
    ST_GeomFromText('POLYGON((-43.80 -23.10, -43.10 -23.10, -43.10 -22.75, -43.80 -22.75, -43.80 -23.10))', 4326)),
('MUNICIPIO', 3, 'Niterói-FAKE', 'RJ-3303302-FAKE',
    ST_GeomFromText('POLYGON((-43.15 -22.95, -43.00 -22.95, -43.00 -22.85, -43.15 -22.85, -43.15 -22.95))', 4326)),
('MUNICIPIO', 3, 'Petrópolis-FAKE', 'RJ-3303906-FAKE',
    ST_GeomFromText('POLYGON((-43.25 -22.60, -43.10 -22.60, -43.10 -22.45, -43.25 -22.45, -43.25 -22.60))', 4326));

-- Municípios MG (pai = 4)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('MUNICIPIO', 4, 'Belo Horizonte-FAKE', 'MG-3106200-FAKE',
    ST_GeomFromText('POLYGON((-44.10 -20.05, -43.85 -20.05, -43.85 -19.80, -44.10 -19.80, -44.10 -20.05))', 4326)),
('MUNICIPIO', 4, 'Uberlândia-FAKE', 'MG-3170206-FAKE',
    ST_GeomFromText('POLYGON((-48.35 -19.00, -48.15 -19.00, -48.15 -18.80, -48.35 -18.80, -48.35 -19.00))', 4326));

-- Bairros/zonas para São Paulo-FAKE (pai = 5, o município SP)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('BAIRRO_ZONA', 5, 'Centro-SP-FAKE', 'SP-CENTRO-FAKE',
    ST_GeomFromText('POLYGON((-46.65 -23.56, -46.60 -23.56, -46.60 -23.52, -46.65 -23.52, -46.65 -23.56))', 4326)),
('BAIRRO_ZONA', 5, 'Zona Leste-FAKE', 'SP-LESTE-FAKE',
    ST_GeomFromText('POLYGON((-46.55 -23.60, -46.36 -23.60, -46.36 -23.45, -46.55 -23.45, -46.55 -23.60))', 4326)),
('BAIRRO_ZONA', 5, 'Zona Norte-FAKE', 'SP-NORTE-FAKE',
    ST_GeomFromText('POLYGON((-46.75 -23.48, -46.55 -23.48, -46.55 -23.36, -46.75 -23.36, -46.75 -23.48))', 4326)),
('BAIRRO_ZONA', 5, 'Zona Oeste-FAKE', 'SP-OESTE-FAKE',
    ST_GeomFromText('POLYGON((-46.83 -23.60, -46.65 -23.60, -46.65 -23.48, -46.83 -23.48, -46.83 -23.60))', 4326)),
('BAIRRO_ZONA', 5, 'Zona Sul-FAKE', 'SP-SUL-FAKE',
    ST_GeomFromText('POLYGON((-46.75 -23.79, -46.55 -23.79, -46.55 -23.60, -46.75 -23.60, -46.75 -23.79))', 4326));

-- Bairros para Rio-FAKE (pai = 9)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('BAIRRO_ZONA', 9, 'Centro-RJ-FAKE', 'RJ-CENTRO-FAKE',
    ST_GeomFromText('POLYGON((-43.20 -22.92, -43.15 -22.92, -43.15 -22.88, -43.20 -22.88, -43.20 -22.92))', 4326)),
('BAIRRO_ZONA', 9, 'Zona Sul-RJ-FAKE', 'RJ-SUL-FAKE',
    ST_GeomFromText('POLYGON((-43.30 -23.05, -43.15 -23.05, -43.15 -22.95, -43.30 -22.95, -43.30 -23.05))', 4326));

-- Bairros para BH-FAKE (pai = 12)
INSERT INTO regioes_eleitorais (nivel, regiao_pai_id, nome_regiao, codigo_ibge, geometria) VALUES
('BAIRRO_ZONA', 12, 'Centro-BH-FAKE', 'MG-BH-CENTRO-FAKE',
    ST_GeomFromText('POLYGON((-43.96 -19.93, -43.92 -19.93, -43.92 -19.90, -43.96 -19.90, -43.96 -19.93))', 4326));
