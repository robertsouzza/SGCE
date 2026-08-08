---
name: sgce-06-modulo-tempo-real-superadmin
description: Gera dois módulos do backend SGCE — tempo-real (heartbeat de localização de membro em campo, publish no Redis, WebSocket/STOMP entregando ao dashboard escopado por partido e por região, respeitando o toggle "modo campo" do voluntário) e o break-glass dual-control do Super Administrador da Plataforma (SolicitacaoAcessoSuporte com aprovação de segundo Super Admin ou fallback para ADMIN do partido alvo, AcessoSuporteLog com expiração automática). Sétima e última skill de backend do roadmap, roda após sgce-05-modulo-consentimento. Use quando o usuário quiser implementar a localização em tempo real das equipes, o dashboard WebSocket ou o fluxo de acesso excepcional do Super Admin.
---

# sgce-06-modulo-tempo-real-superadmin — Redis+WebSocket + Break-glass dual-control

## Contexto

Sétima skill (última do backend). Junta dois módulos porque ambos são "transversais" e usam o mesmo cliente Redis: `tempo-real` (heartbeats + WebSocket) e a finalização de `auditoria` com o break-glass dual-control do Super Admin.

**Assume:** skills 00–05 (backend + partido/candidato + equipe/membro + financeiro + eleitores + consentimento prontos; `ConsentimentoMembro` da skill 05 valida se o voluntário aceitou rastreamento).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — Módulo Tempo Real + Módulo Auditoria (`AcessoSuporteLog`) + matriz de permissões (Super Admin)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-18, RF-19, RF-20, RF-22, RF-23, RNF-01, RNF-15
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-09 (dual-control break-glass), D-10 (toggle "modo campo" do voluntário)

## Passos

1. **Migrations** (`V23__localizacao_equipe.sql`, `V24__acesso_suporte.sql`):
   - `localizacao_equipe_tempo_real`: histórico opcional para replay (fonte de verdade é Redis). `membro_id`, `partido_id` (derivado), `geolocalizacao geometry(Point, 4326)`, `timestamp`, `status_conexao` (ONLINE, OFFLINE_COLETANDO). Índice `(partido_id, timestamp DESC)`.
   - `solicitacoes_acesso_suporte`: `id, solicitante_id, partido_alvo_id, motivo, escopo, criada_em, status (PENDENTE/APROVADA/NEGADA/EXPIRADA), aprovador_id NULL, aprovada_em NULL, aprovacao_fallback BOOL`.
   - `acessos_suporte_log`: `id, solicitacao_id, usuario_id, partido_id_acessado, escopo_acesso, iniciado_em, expira_em, finalizado_em`. **Sem RLS** — só SUPER_ADMIN acessa via endpoint dedicado com filtro na camada de aplicação.

2. **Módulo `tempo-real`** (`com.campanha.temporeal`):
   - **Domain:** `LocalizacaoEquipe` (VO com `membroId`, `partidoId`, `ponto`, `timestamp`, `statusConexao`).
   - **Application:**
     - Ports in: `RegistrarHeartbeatUseCase`, `EncerrarModoCampoUseCase`.
     - Ports out: `LocalizacaoPublisherPort` (publica no Redis).
   - **Infrastructure:**
     - `LocalizacaoController` — `POST /api/tempo-real/heartbeat`. **Valida** `ConsentimentoMembro.consentimentoRastreamentoAtivo(usuarioId)`; se falso (voluntário não aceitou termo ou toggle desligado), rejeita 403 `MODO_CAMPO_INATIVO`. Se ok, publica no Redis via `LocalizacaoPublisherPort`.
     - `RedisPublisherAdapter` — usa `RedisTemplate<String, LocalizacaoEventoDto>`, canal `sgce:tempo-real:partido:{partidoId}`.
     - `DashboardWebSocketConfig` + `DashboardStompController`: subscribe em `/topic/tempo-real/{partidoId}`. `RedisSubscriberBridge` assina o canal Redis e retransmite via STOMP para os assinantes daquele partido.
     - Escopo de segurança: filtro STOMP valida que o cliente autenticado tem `partido_id` igual ao do canal — impede um usuário do partido A subscrever `/topic/tempo-real/{partidoB}`.

3. **RF-20 (3 estados no mapa)** — evento emitido:
   - Estado 1 (`SEM_DADOS`): calculado no frontend, não emite nada.
   - Estado 2 (`INTENCAO_CALCULADA`): quando `SincronizarLoteOfflineUseCase` (skill 04) processa uma abordagem, publica `abordagem_sincronizada` no canal `sgce:tempo-real:partido:{id}` com `regiao_id` — o frontend recalcula gradient.
   - Estado 3 (`EQUIPE_EM_CAMPO_SEM_CONEXAO`): agendado (`@Scheduled` a cada 30s) — se última localização de um membro em modo campo tem >2min, emite `membro_offline_coletando` com `membro_id`, `ultima_regiao_id`.

4. **Break-glass dual-control (D-09)** — no módulo `auditoria` (`com.campanha.auditoria`):
   - **Domain:** `SolicitacaoAcessoSuporte` (com transições `aprovar`, `negar`, `expirar`, `finalizar`), `AcessoSuporteLog`.
   - **Application:**
     - Ports in: `AbrirSolicitacaoSuporteUseCase`, `AprovarSolicitacaoSuporteUseCase`, `NegarSolicitacaoSuporteUseCase`, `IniciarSessaoSuporteUseCase`, `FinalizarSessaoSuporteUseCase`.
     - `AprovarSolicitacaoSuporteUseCase` valida:
       - Aprovador é `SUPER_ADMIN_PLATAFORMA` **diferente** do solicitante, OU
       - Fallback: se `count(SUPER_ADMIN ativos) == 1`, o `ADMIN` do `partido_alvo` pode aprovar (marca `aprovacao_fallback=true`).
     - `IniciarSessaoSuporteUseCase`: só se solicitação está APROVADA e não expirou; cria `AcessoSuporteLog` com `expira_em = now + 2h`; retorna token de sessão de suporte que o filtro `TenantContext` reconhece (`SET LOCAL app.current_partido_id = partido_alvo` mesmo o usuário sendo SUPER_ADMIN).
   - **Infrastructure:**
     - `SolicitacaoSuporteController` (endpoints para abrir, listar, aprovar, negar).
     - `SessaoSuporteController` (`POST /iniciar`, `POST /finalizar`).
     - `SessaoSuporteFilter`: se o request tem header/token de sessão de suporte válida, sobrepõe o `TenantContext` para o partido alvo — **e loga cada request** com o `escopo_acesso`.
   - Notificações: por ora, só log estruturado. Integração e-mail/push fica fora de escopo.

5. **Permissões**:
   - Heartbeat: `MEMBRO_EQUIPE`, `LIDER_EQUIPE`, com `ConsentimentoMembro` ativo.
   - WebSocket `/topic/tempo-real/{partidoId}`: só usuários daquele partido (qualquer perfil autorizado a ver o dashboard — `CANDIDATO`, `ADMIN`, `LIDER_EQUIPE`).
   - `SUPER_ADMIN` no dashboard genérico: só métricas operacionais agregadas de todos os partidos (uso, erros, uptime) via canal separado `/topic/metricas-plataforma` — **nunca** conteúdo sensível fora de sessão de suporte.
   - Solicitação de suporte: `SUPER_ADMIN` abre; outro `SUPER_ADMIN` ou fallback `ADMIN` do partido alvo aprova.

6. **Testes**:
   - Unit: transições de `SolicitacaoAcessoSuporte` (aprovar respeita regras de "outro Super Admin" e "fallback").
   - Application: `IniciarSessaoSuporteUseCase` rejeita solicitação PENDENTE, NEGADA, EXPIRADA.
   - Integration (Testcontainers Postgres+Redis): heartbeat → Redis → WebSocket cliente recebe. Filtro STOMP bloqueia cross-tenant. Sessão de suporte permite Super Admin ver dado de partido alvo, e o `AcessoSuporteLog` grava cada request feito na sessão.
   - Teste temporal: `@Scheduled` de "equipe em campo sem conexão" emite evento após 2min sem heartbeat (usar `TestClock` ou `@MockBean` de `Clock`).

## Definition of Done (verificável)

```bash
cd backend && ./mvnw test   # verde

# Manual:
# --- Tempo real ---
# 1. Login como MEMBRO_EQUIPE (voluntário) sem ConsentimentoMembro ativo:
#    POST /api/tempo-real/heartbeat → 403 MODO_CAMPO_INATIVO
# 2. Ativa consentimento + toggle modo campo (skill 05)
# 3. POST /api/tempo-real/heartbeat → 200
# 4. Cliente WebSocket subscribe /topic/tempo-real/{partidoId} (STOMP) recebe o evento em <2s
# 5. Cliente WebSocket de OUTRO partido tenta subscribe → conexão rejeitada
#
# --- Break-glass ---
# 6. Login como SUPER_ADMIN A → POST /api/suporte/solicitacoes {partido_alvo, motivo, escopo} → 201 PENDENTE
# 7. Login como SUPER_ADMIN B → PATCH /api/suporte/solicitacoes/{id}/aprovar → 200 APROVADA
# 8. Login como SUPER_ADMIN A → POST /api/suporte/sessoes/iniciar {solicitacao_id} → 200, retorna token de sessão
# 9. GET /api/eleitores com header X-Session-Suporte: <token> → retorna eleitores do partido alvo (Super Admin agora vê o partido); mesma request sem o header → retorna vazio (Super Admin sem tenant)
# 10. GET /api/suporte/logs → lista os acessos, incluindo cada request feito com o token
# 11. Aguarda expiração (ou ajusta expira_em no teste) → GET com token expirado retorna 401 SESSAO_SUPORTE_EXPIRADA
# 12. Cenário fallback: apagar SUPER_ADMIN B; ADMIN do partido alvo aprova; sessão inicia com aprovacao_fallback=true no log
```

## Notas para skills seguintes

- Skill 08 (frontend campo + dashboard) consome `/topic/tempo-real/{partidoId}` via `RealtimeService` do `core/` (criado na skill 07) e implementa a UI dos 3 estados no mapa.
- Backend está funcionalmente completo após esta skill.
