-- V26: adiciona campo 'ativo' em candidatos para soft delete.
-- Candidato inativado permanece no banco (preserva histórico de abordagens
-- e prestação de contas), apenas sai da listagem padrão. Reversível via
-- endpoint /candidatos/{id}/ativar.
ALTER TABLE candidatos
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_candidatos_ativo ON candidatos (partido_id, ativo);
