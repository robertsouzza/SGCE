-- Adiciona GOVERNADOR à lista de cargos válidos da constraint CHECK de candidatos.
-- O enum Cargo (Java) já foi atualizado; sem esta migration o insert falha com
-- ConstraintViolation "candidatos_cargo_valido".
ALTER TABLE candidatos DROP CONSTRAINT IF EXISTS candidatos_cargo_valido;
ALTER TABLE candidatos ADD CONSTRAINT candidatos_cargo_valido CHECK (
    cargo IN ('PRESIDENTE','GOVERNADOR','SENADOR','DEPUTADO_FEDERAL','DEPUTADO_ESTADUAL','PREFEITO','VEREADOR')
);
