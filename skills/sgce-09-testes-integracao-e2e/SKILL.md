---
name: sgce-09-testes-integracao-e2e
description: Adiciona a suíte de testes de integração cruzada (Testcontainers com Postgres+PostGIS e Redis reais, testes multi-tenant dedicados que provam RLS efetiva, testes de idempotência de sincronização offline) e E2E com Playwright (login por perfil, cadastro completo de eleitor com consentimento, ciclo offline→sincronização→dashboard, break-glass dual-control ponta a ponta, matriz de permissões visível na UI). Décima skill do roadmap, roda após sgce-08-frontend-campo-dashboard. Use quando o usuário quiser blindar o sistema contra regressões, provar que o isolamento multi-tenant não quebra, ou automatizar os fluxos críticos manuais em E2E.
---

# sgce-09-testes-integracao-e2e — Testcontainers + Playwright + multi-tenant

## Contexto

Décima skill. Consolida a estratégia de teste. Alguns testes já foram criados nas skills 01–08 (unit, ArchUnit, integração pontual por módulo). Esta skill adiciona os testes **transversais e de regressão** que só fazem sentido quando o sistema inteiro existe.

**Assume:** skills 00–08 (backend + frontend completos e funcionais localmente).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 6 (estratégia de testes completa)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RNF-17, RNF-18, RNF-19, RNF-20
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-04 (conflito sync), D-05 (RLS + pool), D-09 (break-glass dual-control)

## Passos

1. **Backend — teste multi-tenant dedicado (RNF-18)** em `backend/src/test/java/com/campanha/multitenancy/MultiTenantIsolationTest.java`:
   - `@SpringBootTest` + `@Testcontainers` sobe Postgres+PostGIS real.
   - Cenário 1: popula 2 partidos, 5 candidatos e 10 eleitores em cada. Login como `ADMIN` do partido A; qualquer `SELECT` retorna só dados do A.
   - Cenário 2: tenta bypass — chamada JPA direta (sem passar pelo `TenantFilter`) — o `TenantAwareTransactionAspect` **precisa** ainda aplicar `SET LOCAL`; senão o teste falha. Isto blinda contra desenvolvedor futuro que "esqueça" o filtro.
   - Cenário 3: valida cross-tenant via SQL raw — `SELECT` sem `WHERE` retorna 0 linhas do outro tenant (RLS pegou).
   - Cenário 4: usuário SUPER_ADMIN sem sessão de suporte — `SELECT` em tabelas de negócio retorna vazio (tenant nulo).
   - Cenário 5: SUPER_ADMIN com sessão de suporte ativa para partido A — vê partido A, não vê partido B.
   - **O teste falha o build se qualquer cenário quebrar.**

2. **Backend — teste de idempotência de sincronização (RNF-19)** em `com/campanha/eleitores/SincronizacaoIdempotenciaTest.java`:
   - Envia mesmo payload 3x, valida que só uma linha foi criada.
   - Envia lote com metade das operações repetidas — só as novas são processadas.
   - Envia com `client_op_id` de mais de 30 dias — reprocessa (não é mais idempotente).
   - Envia conflito real, valida que `LogAuditoria` tem `dados_antes` preenchido.

3. **Frontend — Playwright (RNF-19, RNF-20)** em `frontend/e2e/`:
   - `playwright.config.ts`: 3 projetos (chromium, mobile-chrome com emulação, mobile-safari). `webServer` que garante o `docker-compose up` está no ar.
   - **Fixtures** em `e2e/fixtures/`:
     - `loginComo(page, perfil)`: helper que loga como um dos perfis usando seeds de teste (SUPER_ADMIN, ADMIN, GERENTE_FINANCEIRO, SECRETARIO, LIDER_EQUIPE, MEMBRO_EQUIPE, CANDIDATO). Depende de uma migration Flyway extra `db/migration/e2e/` só carregada com profile `e2e` — insere os 7 usuários sintéticos.
   - **Test suites**:
     - `auth.spec.ts`: login/logout por perfil, sidebar dinâmica por perfil (elementos visíveis/ocultos).
     - `matriz-permissoes.spec.ts`: para cada perfil, tenta acessar rotas proibidas → redirecionado; permitidas → carrega.
     - `cadastro-eleitor-completo.spec.ts`: fluxo integral online — MEMBRO_EQUIPE cadastra eleitor + abordagem + intenção múltipla + consentimento por assinatura (`page.locator('canvas').first().click({position:...})` simula desenho) + consentimento por QR wa.me (valida que a URL gerada tem formato `wa.me/...`).
     - **`ciclo-offline-sync-dashboard.spec.ts`** (fluxo mais crítico):
       ```
       - Login MEMBRO_EQUIPE
       - context.setOffline(true)
       - Cadastra 3 eleitores → badges "pendente"
       - context.setOffline(false)
       - Aguarda drena outbox (poll no badge virar "sincronizado", timeout 15s)
       - Nova context loga como ADMIN em outra tab, dashboard mostra +3 eleitores
       ```
     - `financeiro-fluxo-aprovacao.spec.ts`: SECRETARIO lança despesa; GERENTE aprova; PDF baixa.
     - `break-glass.spec.ts`: SUPER_ADMIN A abre solicitação; SUPER_ADMIN B aprova; A inicia sessão; A vê dado do partido alvo; log de acesso registra as requests.
     - `revogacao-anonimiza.spec.ts`: cadastra eleitor + consentimento; revoga; nome vira anonimizado; agregado por região preserva contagem.

4. **Backend — teste de segurança WebSocket cross-tenant**: valida que cliente STOMP autenticado como partido A é rejeitado ao tentar subscribe em `/topic/tempo-real/{partidoB}`.

5. **Cobertura JaCoCo (RNF-17)**: adicionar `jacoco-maven-plugin` com gate no `verify` — falha o build se cobertura de `com.campanha.*.domain` ou `com.campanha.*.application` cair abaixo de 70%.

6. **Documentação**: `frontend/e2e/README.md` explicando como rodar Playwright localmente (`npx playwright test`, `--ui`, `--headed`), como debugar (`--debug`) e como atualizar seeds.

## Definition of Done (verificável)

```bash
# Backend
cd backend && ./mvnw verify   # inclui unit + integration + Testcontainers + JaCoCo gate

# Frontend
cd frontend && npm test                    # Jest
cd frontend && npx playwright install      # 1a vez
docker-compose up -d
cd frontend && npx playwright test         # todos verdes

# Regressão intencional para provar que o teste multi-tenant pega:
# 1. Comente `SET LOCAL app.current_partido_id = ...` no TenantAwareTransactionAspect
# 2. Rode ./mvnw test -Dtest=MultiTenantIsolationTest
# 3. Deve falhar (mostra que a RLS não estava sendo aplicada)
# 4. Restaure o SET LOCAL
```

## Notas para skills seguintes

- Skill 10 (CI/CD) vai executar `./mvnw verify` e `npx playwright test` como gates do pipeline.
- Se um destes testes falha depois, **investigar a regressão, não desabilitar o teste** (regra da meta-skill).
