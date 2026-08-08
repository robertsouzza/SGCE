---
name: sgce-04-modulo-eleitores
description: Gera o módulo eleitores do backend SGCE — Eleitor com geolocalização PostGIS, Abordagem (visita/interação), IntencaoVoto N:N com Candidato (1 ou vários na mesma abordagem), RegiaoEleitoral hierárquica (schema + seed sintético de 3 UFs fake), e o endpoint SincronizacaoController que recebe lote acumulado offline e resolve conflitos por last-write-wins com dados_antes no LogAuditoria. Quinta skill do roadmap, roda após sgce-03-modulo-financeiro. Use quando o usuário quiser cadastrar eleitores, registrar abordagem em campo, ou implementar o endpoint de sincronização offline.
---

# sgce-04-modulo-eleitores — Eleitor, Abordagem, IntencaoVoto, RegiaoEleitoral, Sync offline

## Contexto

Quinta skill. Núcleo do trabalho de campo. Cobre RF-12 a RF-14, RF-17 (offline) do lado backend. O endpoint de sincronização é o que o app mobile chama quando reconecta — precisa ser robusto a conflitos.

**Assume:** skills 00–03 (backend + partido/candidato + equipe/membro + financeiro prontos).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 1 (Eleitor, Abordagem, IntencaoVoto, RegiaoEleitoral) + seção 2 (offline-first)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-12 a RF-14, RF-17, RNF-19
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-04 (last-write-wins + relógio errado), D-06 (malha IBGE — seed sintético só), D-11 (fuso UTC + timestamp_local separado)

## Passos

1. **Migrations** (`V16__eleitores.sql`, `V17__abordagens_intencoes.sql`, `V18__regioes_eleitorais.sql`, `V19__seed_regioes_sinteticas.sql`):
   - `eleitores`: `partido_id` (partido que cadastrou primeiro), `titulo_eleitor` UNIQUE por partido, `geolocalizacao geometry(Point, 4326)`. RLS habilitada.
   - `abordagens`: `sincronizado bool DEFAULT false`, `timestamp_local TIMESTAMPTZ`, `timestamp_sincronizacao TIMESTAMPTZ`, `geolocalizacao_abordagem geometry(Point, 4326)`.
   - `intencoes_voto`: N:N `abordagem_id` ↔ `candidato_id`, enum `intencao` (FAVORAVEL, INDECISO, CONTRARIO, HOSTIL).
   - `regioes_eleitorais`: schema completo com `nivel` (PAIS, ESTADO, MUNICIPIO, BAIRRO_ZONA), `regiao_pai_id` auto-FK, `codigo_ibge`, `geometria geometry(Polygon, 4326)`. **Sem RLS** (dados públicos, mesmos para todos os partidos).
   - Seed sintético em `V19`: 1 país fake, 3 estados fake (`SP-FAKE`, `RJ-FAKE`, `MG-FAKE`), ~10 municípios fake, ~30 bairros/zonas fake. Polígonos de bounding box arbitrário. Cabeçalho SQL com comentário: **"DADOS SINTÉTICOS PARA DEV. Malha IBGE real via scripts/import-ibge.sh (skill 10)."**
   - Índices GiST em `geolocalizacao` (Eleitor, Abordagem) e `geometria` (RegiaoEleitoral).

2. **Módulo `eleitores`** (`com.campanha.eleitores`):
   - **Domain:** `Eleitor`, `Abordagem`, `IntencaoVoto` (VO), `RegiaoEleitoral`. Método `Eleitor.anonimizar()` (preparação para D-02 usado no skill 05 — retorna cópia com PII nula, exceto `id`, `partido_id`, `titulo_eleitor` hash).
   - **Application:**
     - Ports in: `CadastrarEleitorUseCase`, `RegistrarAbordagemUseCase`, `RegistrarIntencaoVotoUseCase`, `SincronizarLoteOfflineUseCase`, `ConsultarRegiaoPorGeoUseCase` (retorna a região no nível mais fino para uma coordenada).
     - Ports out: `EleitorRepositoryPort`, `AbordagemRepositoryPort`, `RegiaoGeoPort` (consultas PostGIS: `ST_Contains`).
   - **Infrastructure:**
     - `EleitorController`, `AbordagemController`, `SincronizacaoController`.
     - `EleitorJpaAdapter` com Hibernate Spatial (`org.locationtech.jts.geom.Point`).

3. **Endpoint de sincronização (`POST /api/sincronizacao/lote`)** — o coração desta skill:
   - Payload: array de operações, cada uma com `entidade` (eleitor/abordagem/intencao/consentimento — este último tratado pela skill 05), `operacao` (CREATE/UPDATE), `payload`, `timestamp_local`, `client_op_id` (idempotência).
   - **Idempotência:** rejeita silenciosamente se `client_op_id` já foi processado nas últimas 30 dias (tabela `sync_op_log`).
   - **Conflito (D-04):** para `eleitor`, chave natural é `titulo_eleitor + partido_id`. Se existe e o `timestamp_local` do incoming é **posterior** ao do existente:
     - Grava o estado atual como `dados_antes` no `LogAuditoria` (via `AuditoriaAspect`).
     - Sobrescreve.
     - Notifica de forma assíncrona (skill 06 tratará via WebSocket) o usuário que perdeu.
   - **Relógio errado (D-04):** se `timestamp_local` do incoming está mais de **24h no futuro** em relação ao `timestamp_sincronizacao` (agora do servidor), rejeita com `422 Unprocessable Entity` e código `CLOCK_SKEW` — o app precisa mostrar mensagem clara para o usuário verificar o relógio.
   - Response: por operação, retorna `{client_op_id, status: OK|CONFLICT_RESOLVED|CLOCK_SKEW|VALIDATION_ERROR, server_id?}`.

4. **Cálculo de região por geoloc**: `ConsultarRegiaoPorGeoUseCase` roda query PostGIS `SELECT id FROM regioes_eleitorais WHERE ST_Contains(geometria, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) ORDER BY nivel DESC LIMIT 1;` — retorna a região mais fina que contém o ponto.

5. **Permissões (RF-21)**:
   - `MEMBRO_EQUIPE`: cria/edita Eleitor e Abordagem (região dele). Não deleta.
   - `LIDER_EQUIPE`: mesmo + visualização completa da região da equipe.
   - `ADMIN`: total dentro do partido.
   - `CANDIDATO`: só agregados/anonimizados (endpoint separado, `GET /api/eleitores/agregado`, sem PII).
   - `GERENTE_FINANCEIRO`/`SECRETARIO`: 403 no módulo eleitores.

6. **Testes**:
   - Unit: `Eleitor.anonimizar()` remove PII mas preserva `titulo_eleitor_hash`.
   - Application: `SincronizarLoteOfflineUseCase` — cenários: novo, update sem conflito, update com conflito (last-write-wins), clock skew, idempotência (repetir mesmo `client_op_id`).
   - Integration (Testcontainers): sync com 100 operações mistas em lote; validar consistência final.
   - Contract: matriz de permissões.

## Definition of Done (verificável)

```bash
cd backend && ./mvnw test   # verde, incluindo SincronizacaoOfflineIntegrationTest

# Manual:
# 1. Login como MEMBRO_EQUIPE
# 2. POST /api/eleitores com nome, título, endereço, geoloc → 201
# 3. GET /api/eleitores/{id}/regiao → retorna id da região BAIRRO_ZONA calculada por PostGIS
# 4. POST /api/abordagens com intencoes_voto=[{candidato_id:X, intencao:FAVORAVEL}, {candidato_id:Y, intencao:INDECISO}] → 201
# 5. POST /api/sincronizacao/lote com array de 3 operações (1 novo eleitor, 1 abordagem, 1 update do eleitor criado) → 200 com status por operação
# 6. Repetir a mesma chamada → todas OK (idempotência), sem duplicidade
# 7. Simular conflito: 2 payloads com mesmo título e timestamps_local diferentes → o mais recente vence, o outro vira dados_antes no LogAuditoria
# 8. Payload com timestamp_local 48h no futuro → 422 CLOCK_SKEW
```

## Notas para skills seguintes

- Skill 05 (`consentimento`) usa `Abordagem` como FK e `Eleitor.anonimizar()` no fluxo de revogação.
- Skill 06 (tempo real) publica no Redis o evento `abordagem_sincronizada` para o dashboard reagir.
- Skill 08 (frontend campo) monta a UI de cadastro/abordagem e implementa o cliente do endpoint de sincronização.
