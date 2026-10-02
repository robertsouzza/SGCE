-- V27: adiciona campo 'ativo' em equipes para soft delete.
ALTER TABLE equipes
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_equipes_ativo ON equipes (partido_id, ativo);
