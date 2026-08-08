# SGCE — Sistema de Gestão de Campanha Eleitoral 2026

> **Aviso — sistema real, não é portfólio nem exercício.**
> Destinado a uso em campanhas eleitorais brasileiras de 2026 (presidente, senadores, deputados federais e estaduais, prefeitos e vereadores). Todo dado de eleitor (nome, endereço, WhatsApp, opinião política) é dado sensível pela LGPD (art. 11) e deve ser tratado como tal desde o primeiro commit — **nunca** subir dados reais para o repositório, mesmo em seed/fixture/teste.

## O que é

Plataforma multi-tenant por partido para gerenciar uma campanha eleitoral ponta a ponta:

- **Cadastro de partidos e candidatos**, com regras de cargo/UF/município no domínio.
- **Gestão financeira** (recursos, despesas categorizadas, fluxo de aprovação, relatório em PDF e JSON).
- **Equipes de campo** (líderes, membros, vinculação a candidatos).
- **Captação de eleitores em campo** com funcionamento **100% offline** (Angular PWA + IndexedDB + Background Sync), sincronizando ao reconectar com resolução de conflito por last-write-wins auditada.
- **Consentimento LGPD** com duas flags independentes (dados vs marketing WhatsApp), captura por assinatura em tela ou por QR code com deep-link `wa.me`, e anonimização automática na revogação (preserva agregados, remove PII).
- **Dashboard em tempo real** com mapa de calor hierárquico (ajustado ao cargo do candidato: país → estado → município → bairro/zona), localização ao vivo das equipes em campo via WebSocket/STOMP, e três estados visuais por região (sem dados / intenção calculada / equipe em campo sem conexão).
- **Break-glass dual-control** para o Super Administrador da Plataforma — acesso excepcional a dados de um partido exige aprovação de segundo Super Admin (ou fallback pelo Admin do partido alvo), com log completo e expiração automática de sessão.

Multi-tenancy é garantida em nível de banco de dados via **PostgreSQL Row-Level Security** (`SET LOCAL app.current_partido_id` dentro de transação), não só na camada de aplicação — um teste de integração dedicado quebra o build se essa garantia enfraquecer.

## Estado atual do repositório

Este repositório contém **apenas a especificação e as skills de geração** — o código do backend e do frontend ainda não foi gerado. As decisões técnicas, o modelo de dados, os requisitos funcionais/não funcionais e as decisões que fecharam ambiguidades estão documentados em [skills/sgce-fullstack/references/](skills/sgce-fullstack/references/).

O sistema é gerado em **11 passos verificáveis** (uma skill Claude Code por passo), cada um com Definition of Done executável por comando. Roadmap completo em [skills/sgce-fullstack/SKILL.md](skills/sgce-fullstack/SKILL.md).

### Roadmap de geração

| # | Skill | Status | O que produz |
|---|---|---|---|
| 00 | [sgce-00-infra-base](skills/sgce-00-infra-base/SKILL.md) | ✅ concluída | Monorepo + `docker-compose.yml` de infraestrutura (Postgres+PostGIS, Redis, MinIO) |
| 01 | [sgce-01-backend-core](skills/sgce-01-backend-core/SKILL.md) | ⏳ pendente | Spring Boot + módulos `shared` (RLS, CORS, OpenAPI, S3), `autenticacao` (JWT em cookie httpOnly + CSRF), `auditoria` + ArchUnit |
| 02 | [sgce-02-modulo-partido-equipe](skills/sgce-02-modulo-partido-equipe/SKILL.md) | ⏳ pendente | Partido, Candidato, Equipe, MembroEquipe, EquipeCandidato |
| 03 | [sgce-03-modulo-financeiro](skills/sgce-03-modulo-financeiro/SKILL.md) | ⏳ pendente | Recurso, Despesa, PagamentoEquipe, fluxo de aprovação, relatório PDF/JSON |
| 04 | [sgce-04-modulo-eleitores](skills/sgce-04-modulo-eleitores/SKILL.md) | ⏳ pendente | Eleitor, Abordagem, IntencaoVoto, RegiaoEleitoral + endpoint de sincronização offline |
| 05 | [sgce-05-modulo-consentimento](skills/sgce-05-modulo-consentimento/SKILL.md) | ⏳ pendente | ConsentimentoLGPD + anonimização + deep-link `wa.me` + stub WhatsApp |
| 06 | [sgce-06-modulo-tempo-real-superadmin](skills/sgce-06-modulo-tempo-real-superadmin/SKILL.md) | ⏳ pendente | WebSocket/Redis + break-glass dual-control do Super Admin |
| 07 | [sgce-07-frontend-core-gestao](skills/sgce-07-frontend-core-gestao/SKILL.md) | ⏳ pendente | Angular PWA + core (auth/realtime/offline) + features de gestão |
| 08 | [sgce-08-frontend-campo-dashboard](skills/sgce-08-frontend-campo-dashboard/SKILL.md) | ⏳ pendente | Eleitores (offline-first) + mapa hierárquico + dashboard tempo real |
| 09 | [sgce-09-testes-integracao-e2e](skills/sgce-09-testes-integracao-e2e/SKILL.md) | ⏳ pendente | Testcontainers + Playwright + teste dedicado de isolamento multi-tenant |
| 10 | [sgce-10-deploy-cicd](skills/sgce-10-deploy-cicd/SKILL.md) | ⏳ pendente | Dockerfiles finais + `docker-compose.yml` unificado + GitHub Actions + README final |

Cada skill tem bloco `Assume:` explícito com o que a anterior entrega. Pular skills quebra o DoD da seguinte.

**Subir só a infraestrutura (após skill 00):**

```bash
docker compose up -d postgres redis minio minio-init
# Postgres:  localhost:5432 (ou SGCE_POSTGRES_PORT do seu .env)
# Redis:     localhost:6379
# MinIO API: http://localhost:9000
# MinIO UI:  http://localhost:9001 (minioadmin / minioadmin)
```

## Stack técnica

- **Backend:** Java 21 + Spring Boot 3.x (arquitetura hexagonal, monólito modular, ArchUnit trava fronteiras), Spring Security + JWT em cookie httpOnly, Spring WebSocket/STOMP, Spring Data JPA + Hibernate Spatial, springdoc-openapi, Flyway.
- **Banco:** PostgreSQL + PostGIS, multi-tenancy via `partido_id` + Row-Level Security.
- **Tempo real:** Redis Pub/Sub, WebSocket/STOMP.
- **Storage:** MinIO em dev (compose) → S3-compatível em produção, adapter único via AWS SDK.
- **Frontend:** Angular estável mais recente, standalone components, PWA, Dexie.js (IndexedDB) para offline-first, Leaflet para mapa de calor, `signature_pad` para consentimento assinado.
- **Testes:** JUnit 5 + Mockito + Testcontainers + ArchUnit (backend), Jest + Angular Testing Library + Playwright com emulação offline (frontend).
- **Infra:** Docker Compose (dev), Dockerfiles multi-stage (prod), GitHub Actions.

## Como começar

**Enquanto o código ainda não está gerado**, o repositório contém apenas as skills, os documentos de referência e este README.

**Para gerar o sistema**, execute as skills na ordem, começando por [sgce-00-infra-base](skills/sgce-00-infra-base/SKILL.md). Cada skill tem DoD verificável — só avance para a próxima quando a atual passar.

**Uma vez completo o roadmap** (após a skill 10), subir o sistema é um único comando:

```bash
cp .env.example .env   # editar segredos localmente
docker-compose up --build
# abrir http://localhost/
```

## Documentação de referência

Compartilhada por todas as skills em [skills/sgce-fullstack/references/](skills/sgce-fullstack/references/):

- [modelo-tecnico-sistema-campanha.md](skills/sgce-fullstack/references/modelo-tecnico-sistema-campanha.md) — modelo de dados completo, estrutura de pacotes, matriz de permissões, estratégia de testes.
- [requisitos-funcionais-nao-funcionais.md](skills/sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md) — RF-01 a RF-23 e RNF-01 a RNF-20, critério de aceite.
- [decisoes-tomadas.md](skills/sgce-fullstack/references/decisoes-tomadas.md) — 12 decisões que fecharam ambiguidades (QR consentimento, retenção pós-revogação, JWT em cookie, break-glass dual-control, RLS + pool, MinIO/S3, WhatsApp stub, malha IBGE, fuso, versão do stack, geoloc de voluntários, conflito de sync). **Em caso de conflito com os outros dois, este arquivo vence.**

## Avisos legais

- **Multi-tenancy é inegociável.** Nunca implementar funcionalidade que cruze dados entre partidos. A Resolução TSE 23.610 proíbe transferência/venda de banco de dados eleitoral entre partidos, e cruzar dados violaria a finalidade do consentimento LGPD (dado sensível — opinião política).
- O módulo financeiro **complementa** e **não substitui** a prestação de contas oficial no SPCE do TSE.
- O termo de consentimento LGPD utilizado em produção deve ser revisado por advogado especialista antes do primeiro uso real.
- Exportação ou compartilhamento da base de eleitores entre partidos distintos **nunca** é permitida, em nenhuma hipótese.

## Contribuindo

Alterações ao modelo de dados, requisitos ou decisões devem ser feitas nos arquivos em [skills/sgce-fullstack/references/](skills/sgce-fullstack/references/) — todas as skills apontam para lá, então uma edição se propaga automaticamente. Alterações na organização das skills (nova skill, mudança de escopo) devem preservar a cadeia linear de dependências e o DoD verificável.

## Licença

A definir.
