---
name: sgce-03-modulo-financeiro
description: Gera o módulo financeiro do backend SGCE — RecursoFundoEleitoral (fundo eleitoral, fundo partidário, doação), Despesa categorizada com fluxo de aprovação PENDENTE→APROVADO/REJEITADO e upload de comprovante via MinIO/S3, PagamentoEquipe (diária/salário/por abordagem/por visita), e endpoint de relatório em PDF e JSON. Quarta skill do roadmap, roda após sgce-02-modulo-partido-equipe. Use quando o usuário quiser lançar despesas de campanha, aprovar gastos, ou gerar relatório financeiro.
---

# sgce-03-modulo-financeiro — Recurso, Despesa, PagamentoEquipe, Relatório

## Contexto

Quarta skill. Cobre RF-05 a RF-09 (financeiro). Fluxo de aprovação de duas etapas: `SECRETARIO` lança, `GERENTE_FINANCEIRO` aprova.

**Assume:** skills 00, 01, 02 (backend + partido/candidato + equipe/membro prontos; `S3StorageAdapter` do `shared` disponível).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 1 (Recurso, Despesa, PagamentoEquipe) + seção 5 (matriz de permissões: Gerente Financeiro e Secretário)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-05 a RF-09, RNF-13
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-02b (S3StorageAdapter para comprovantes)

## Passos

1. **Migrations** (`V13__recursos.sql`, `V14__despesas.sql`, `V15__pagamentos_equipe.sql`):
   - Todas com `partido_id` derivado do `Candidato` (denormalizado no `INSERT` via trigger ou preenchido pela camada de aplicação), RLS habilitada.
   - `despesas.status` enum `PENDENTE`, `APROVADO`, `REJEITADO`. `aprovado_por`, `aprovado_em` nullable.
   - `pagamentos_equipe.tipo_pagamento` enum `DIARIA`, `SALARIO`, `POR_ABORDAGEM`, `POR_VISITA`.
   - `comprovante_url` (VARCHAR) — só a chave S3, não a URL completa.

2. **Módulo `financeiro`** (`com.campanha.financeiro`):
   - **Domain:** `RecursoFundoEleitoral`, `Despesa` (com método `aprovar(usuarioId)`, `rejeitar(usuarioId, motivo)`, ambos disparam `IllegalStateException` se `status != PENDENTE`), `PagamentoEquipe`. Exceção `SaldoInsuficienteException` opcional (calcular saldo antes de aprovar despesa acima do disponível — decisão de MVP: **calcular mas não bloquear**; retornar warning no response).
   - **Application:**
     - Ports in: `RegistrarRecursoUseCase`, `LancarDespesaUseCase`, `AprovarDespesaUseCase`, `RegistrarPagamentoEquipeUseCase`, `GerarRelatorioFinanceiroUseCase`.
     - Ports out: `RecursoRepositoryPort`, `DespesaRepositoryPort`, `PagamentoEquipeRepositoryPort`, **`ComprovanteStoragePort`** (implementada pelo `S3StorageAdapter` do `shared`).
     - Todos os use cases mutantes: `@Auditavel` + `@Transactional`.
   - **Infrastructure:**
     - `RecursoController`, `DespesaController` (POST cria, PATCH aprova/rejeita, GET lista/detalhe), `RelatorioController` (`GET /api/relatorios/financeiro?formato=pdf|json`).
     - Upload multipart de comprovante: endpoint recebe arquivo, chama `ComprovanteStoragePort.save()`, guarda a chave S3 no BD.
     - JPA adapters padrão.
     - Geração de PDF: dependência leve tipo **OpenPDF** ou **Apache PDFBox**. Template simples: cabeçalho com partido/candidato/período, tabelas de recursos e despesas, totais. `RelatorioPdfGeneratorAdapter`.

3. **Permissões (RF-08, RF-21)**:
   - `SECRETARIO`: lança recurso e despesa (`POST`). **Não** aprova.
   - `GERENTE_FINANCEIRO`: tudo do secretário + aprova/rejeita despesa.
   - `ADMIN`: tudo.
   - `CANDIDATO`: só leitura do resumo (`GET /api/relatorios/financeiro` retornando JSON agregado, sem detalhe de despesa individual).
   - Todos os outros perfis: 403.

4. **Presigned URL para comprovantes**: endpoint `GET /api/despesas/{id}/comprovante-url` retorna URL presigned válida por 5 minutos (frontend baixa direto do MinIO). Backend nunca faz proxy do binário.

5. **Testes**:
   - Unit: transições de status válidas/inválidas em `Despesa.aprovar()`.
   - Application: `AprovarDespesaUseCase` só aceita usuário com perfil correto.
   - Integration (Testcontainers): upload real de comprovante no MinIO, geração de presigned, download.
   - Contract: matriz de permissões (secretário 403 no PATCH de aprovação, gerente 200; candidato só vê resumo).
   - PDF: teste de smoke — gera para um caso conhecido e valida que o arquivo abre e tem N páginas (sem verificar layout, só que não crasha).

## Definition of Done (verificável)

```bash
cd backend && ./mvnw test   # verde

# Manual (backend up):
# 1. Login como ADMIN do partido X
# 2. POST /api/recursos → cria fundo eleitoral R$ 100k
# 3. Login como SECRETARIO
# 4. POST /api/despesas (multipart, com PDF de comprovante) → cria despesa PENDENTE
# 5. Tenta PATCH /api/despesas/{id}/aprovar → 403
# 6. Login como GERENTE_FINANCEIRO
# 7. PATCH /api/despesas/{id}/aprovar → 200, status APROVADO no GET
# 8. GET /api/despesas/{id}/comprovante-url → URL presigned, baixa o PDF original
# 9. GET /api/relatorios/financeiro?formato=pdf → PDF válido baixado
# 10. GET /api/relatorios/financeiro?formato=json → JSON com totais por categoria
```

## Notas para skills seguintes

- Skill 06 (`equipe` em campo) vai criar `PagamentoEquipe` a partir de eventos reais de campo (abordagem/visita). Não duplicar lógica — chamar `RegistrarPagamentoEquipeUseCase`.
- Skill 07 (frontend gestão) monta as telas de despesa/aprovação.
