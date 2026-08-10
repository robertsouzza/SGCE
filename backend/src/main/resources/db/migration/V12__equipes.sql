-- ============================================================
-- V12: equipes, membros de equipe, equipe-candidato (N:N) + RLS
-- ============================================================

CREATE TABLE equipes (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    nome VARCHAR(200) NOT NULL,
    lider_id BIGINT NOT NULL REFERENCES usuarios(id),
    regiao_atuacao VARCHAR(300),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_equipes_partido ON equipes (partido_id, criado_em DESC);

ALTER TABLE equipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE equipes FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON equipes
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);

-- ------------------------------------------------------------

CREATE TABLE membros_equipe (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    equipe_id BIGINT NOT NULL REFERENCES equipes(id),
    funcao VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT membros_equipe_usuario_unico_por_equipe UNIQUE (equipe_id, usuario_id)
);
CREATE INDEX idx_membros_equipe_partido ON membros_equipe (partido_id);
CREATE INDEX idx_membros_equipe_equipe ON membros_equipe (equipe_id);

ALTER TABLE membros_equipe ENABLE ROW LEVEL SECURITY;
ALTER TABLE membros_equipe FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON membros_equipe
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);

-- ------------------------------------------------------------

CREATE TABLE equipe_candidato (
    id BIGSERIAL PRIMARY KEY,
    partido_id BIGINT NOT NULL REFERENCES partidos(id),
    equipe_id BIGINT NOT NULL REFERENCES equipes(id),
    candidato_id BIGINT NOT NULL REFERENCES candidatos(id),
    vigente_desde DATE NOT NULL,
    vigente_ate DATE,
    CONSTRAINT equipe_candidato_unico UNIQUE (equipe_id, candidato_id, vigente_desde)
);
CREATE INDEX idx_equipe_candidato_partido ON equipe_candidato (partido_id);
CREATE INDEX idx_equipe_candidato_equipe ON equipe_candidato (equipe_id);
CREATE INDEX idx_equipe_candidato_candidato ON equipe_candidato (candidato_id);

ALTER TABLE equipe_candidato ENABLE ROW LEVEL SECURITY;
ALTER TABLE equipe_candidato FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON equipe_candidato
    USING (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT)
    WITH CHECK (partido_id = NULLIF(current_setting('app.current_partido_id', TRUE), '')::BIGINT);
