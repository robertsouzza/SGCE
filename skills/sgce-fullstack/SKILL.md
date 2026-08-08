---
name: sgce-fullstack
description: Meta-skill/índice do projeto SGCE (Sistema de Gestão de Campanha Eleitoral 2026). Descreve o projeto, guarda os documentos de referência compartilhados (modelo técnico, requisitos, decisões tomadas) e orienta a ordem de execução das 11 skills numeradas (sgce-00 a sgce-10) que geram o sistema por partes verificáveis. Use esta skill quando o usuário pedir para "gerar o projeto SGCE", "aplicar o SKILL.md desse diretório", perguntar por onde começar o sistema de gestão de campanha eleitoral, ou quando precisar consultar as decisões técnicas/legais já tomadas para o projeto.
---

# SGCE — Meta-skill / Índice do projeto

## O que é o SGCE

**SGCE** (Sistema de Gestão de Campanha Eleitoral 2026) é um sistema **real**, para uso em campanhas eleitorais de verdade (eleições 2026 no Brasil — presidente, senadores, deputados federais e estaduais, prefeitos e vereadores). Não é portfólio nem exercício acadêmico.

Trate qualquer dado de eleitor (nome, endereço, WhatsApp, opinião política) como dado sensível desde o primeiro commit: **nunca commitar dados reais** em seeds/fixtures — só dados sintéticos, com aviso claro no arquivo.

O sistema é **multi-tenant por partido**: múltiplos partidos usam a mesma instalação, cada um com seus candidatos, equipes, financeiro e eleitores completamente isolados. **Nunca implemente funcionalidade que cruze dados entre partidos** — foi avaliado e rejeitado por risco legal (viola Resolução TSE 23.610 e a finalidade do consentimento LGPD). Justificativa completa em `references/modelo-tecnico-sistema-campanha.md`.

## Documentos de referência (leia antes de qualquer skill)

Tudo em `references/` desta pasta é compartilhado por **todas** as skills numeradas. As skills apontam para cá via `../sgce-fullstack/references/`. Se você editar algo aqui, todas as skills veem a mudança.

1. **`references/modelo-tecnico-sistema-campanha.md`** — modelo de dados completo, estrutura de pacotes, matriz de permissões, estratégia de testes.
2. **`references/requisitos-funcionais-nao-funcionais.md`** — RF-01 a RF-23 e RNF-01 a RNF-20, servem de critério de aceite.
3. **`references/decisoes-tomadas.md`** — decisões que fecharam ambiguidades depois da primeira redação dos dois docs acima. **Em caso de conflito, este arquivo vence.**

Se encontrar ambiguidade que os três documentos não cobrem: **pare e pergunte**, não assuma.

## Como o projeto é gerado

Em vez de tentar gerar o sistema todo de uma vez (o que provavelmente falharia em `docker-compose up --build`), o projeto é dividido em **11 skills numeradas**, cada uma com escopo pequeno e **um Definition of Done verificável por comando**. Rode-as na ordem — cada uma assume que a anterior rodou com sucesso.

### Roadmap de skills

| Ordem | Skill | O que produz | DoD (resumo) |
|-------|-------|--------------|--------------|
| 00 | `sgce-00-infra-base` | Monorepo + `docker-compose.yml` só com Postgres+PostGIS, Redis, MinIO | `docker-compose up postgres redis minio` sobe os 3 |
| 01 | `sgce-01-backend-core` | Spring Boot + módulo `shared` (TenantContext, RLS, CORS, OpenAPI, S3) + `autenticacao` (JWT em cookie httpOnly) + `auditoria` esqueleto + ArchUnit | Login gera cookie, endpoint autenticado responde 401/200, Swagger UI, ArchUnit passa |
| 02 | `sgce-02-modulo-partido-equipe` | Módulos `partido` (Partido, Candidato) e `equipe` (Equipe, MembroEquipe, EquipeCandidato) | CRUD funciona; teste multi-tenant com 2 partidos passa |
| 03 | `sgce-03-modulo-financeiro` | Módulo `financeiro` completo (Recurso, Despesa, PagamentoEquipe, aprovação, relatório PDF+JSON) | Cadastra recurso→despesa→aprova→gera PDF (RF-05 a RF-09) |
| 04 | `sgce-04-modulo-eleitores` | Módulo `eleitores` (Eleitor, Abordagem, IntencaoVoto, RegiaoEleitoral com seed sintético) + endpoint de sincronização em lote com last-write-wins | Cadastro com geoloc funciona; batch sync resolve conflito conforme D-04 |
| 05 | `sgce-05-modulo-consentimento` | Módulo `consentimento` (Termo, ConsentimentoLGPD 2-flags, anonimização, TermoConsentimentoMembro, deep-link `wa.me`, WhatsApp stub) | Captura consentimento; revoga dados → eleitor anonimizado, ConsentimentoLGPD e agregados intactos |
| 06 | `sgce-06-modulo-tempo-real-superadmin` | Módulo `tempo-real` (Redis+WebSocket/STOMP) + break-glass dual-control do Super Admin | Heartbeat via Redis chega no WebSocket escopado por partido; dual-control exige 2 aprovações |
| 07 | `sgce-07-frontend-core-gestao` | Angular 20+ PWA, `core/` (auth cookie/CSRF, RealtimeService, OfflineStore/SyncService), features `auth`+`partido`+`equipe`+`financeiro` | Login via cookie ponta a ponta; CRUD de gestão por perfil; Jest passa |
| 08 | `sgce-08-frontend-campo-dashboard` | Features `eleitores` (com QR `wa.me`, canvas assinatura, offline via Dexie), `mapa` (heatmap hierárquico), `dashboard` (WebSocket, 3 estados RF-20) | Cadastro em modo offline via DevTools funciona; sync ao reconectar; dashboard reflete em segundos |
| 09 | `sgce-09-testes-integracao-e2e` | Testcontainers (Postgres+Redis reais), teste multi-tenant dedicado, Playwright E2E dos fluxos críticos | `mvn verify` + `npx playwright test` verdes; multi-tenant quebra se RLS for removida |
| 10 | `sgce-10-deploy-cicd` | Dockerfiles multi-stage, `docker-compose.yml` final com nginx reverso, GitHub Actions, `scripts/import-ibge.sh`, README final | `docker-compose up --build` do zero sobe tudo e o fluxo completo funciona no browser; CI passa |

### Dependências

Linear: cada skill assume tudo o que veio antes.

- **00** → nada anterior.
- **01** assume 00 (containers de infra existem).
- **02–06** assumem 01 (backend core, `shared`, `autenticacao`, `auditoria` prontos).
- **07** assume 01–06 (backend inteiro no ar; frontend consome esses endpoints).
- **08** assume 07 (Angular core, features de gestão, RealtimeService/OfflineStore prontos).
- **09** assume 08 (backend + frontend completos, para rodar E2E de verdade).
- **10** assume 09 (tudo funcionando local, agora empacota para deploy repetível).

Pular skills quebra o Definition of Done da seguinte.

## Regras que valem para todas as skills

- **Idioma:** código em inglês (nomes de classes, métodos, endpoints); comentários e mensagens de erro em português; commits em português; docs em português.
- **Nada de dados reais** em seed/fixture/teste. Sempre sintético, com comentário no topo do arquivo avisando.
- **Nunca cruzar dados entre partidos.** Se um endpoint parecer natural de agregar "todos os partidos" para alguém que não seja Super Admin em modo suporte, pare e pergunte.
- **RLS obrigatório em toda tabela de negócio** que carregue `partido_id`. Usar `SET LOCAL app.current_partido_id` dentro de transação (D-05).
- **Preferir fazer funcionar antes de polir.** Um sistema simples que sobe com `docker-compose up --build` vale mais que um sistema ambicioso que não sobe.
- **Se ArchUnit reprovar, pare e conserte.** Não desabilite o teste para o build passar.

## Como usar esta meta-skill

Se o usuário pedir para "gerar o SGCE" ou "aplicar o SKILL.md desse diretório":

1. Confirme que ele quer executar o roadmap completo (11 passos), ou apenas alguma parte específica.
2. Direcione para a primeira skill aplicável (`sgce-00-infra-base` para começar do zero, ou a skill correspondente à parte que ele quer).
3. Não tente reimplementar o que as skills numeradas fazem — chame-as.
