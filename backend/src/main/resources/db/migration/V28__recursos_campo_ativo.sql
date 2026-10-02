-- V28: adiciona campo 'ativo' em recursos para soft delete.
-- Preserva histórico financeiro — recurso inativado some da listagem padrão
-- mas continua computando (ou não, dependendo de filtro) nos relatórios.
ALTER TABLE recursos
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_recursos_ativo ON recursos (partido_id, ativo);
