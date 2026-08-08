---
name: sgce-08-frontend-campo-dashboard
description: Gera as features de campo e o dashboard tempo real do frontend SGCE — eleitores (cadastro, abordagem, intenção de voto para 1 ou vários candidatos, captura de consentimento por assinatura em canvas ou deep-link wa.me via QR code, funcionamento 100% offline via OfflineStore/SyncService), mapa (Leaflet com heatmap hierárquico ajustado ao cargo do candidato logado), e dashboard em tempo real (subscribe WebSocket/STOMP via RealtimeService, sinalização dos 3 estados no mapa RF-20 — sem dados, intenção calculada, equipe em campo sem conexão). Nona skill do roadmap, roda após sgce-07-frontend-core-gestao. Use quando o usuário quiser implementar as telas de campo, o mapa de calor eleitoral ou o dashboard em tempo real.
---

# sgce-08-frontend-campo-dashboard — Eleitores + Mapa + Dashboard tempo real

## Contexto

Nona skill. Fecha o frontend adicionando as features "sensíveis a campo" (offline-first, WebSocket) e a integração dos serviços `RealtimeService` e `OfflineStore/SyncService` criados na skill 07.

**Assume:** skills 00–07 (backend inteiro + frontend core + features de gestão prontos).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 2 (offline-first) + seção 4 (front-end)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-04, RF-12 a RF-20, RNF-01, RNF-08, RNF-10, RNF-11
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-01 (QR wa.me), D-02 (revogação anonimiza), D-04 (last-write-wins), D-10 (toggle modo campo)

## Passos

1. **Dependências**: `leaflet`, `@types/leaflet`, `leaflet.heat` (plugin de heatmap). Opcionalmente Mapbox GL JS — decisão: **Leaflet** (sem token, mais simples para dev; troca fácil no futuro se precisar).

2. **Feature `eleitores`**:
   - Lista de eleitores da região do membro (`GET /api/eleitores?regiao_id=...`).
   - **Cadastro (fluxo offline-first)**: form com nome, endereço, geoloc (auto via `navigator.geolocation`), telefone, título/zona/seção.
     - Ao submeter: **grava direto no `OfflineStore.eleitores_locais`** e enfileira no `outbox` com `client_op_id = uuid()`. Se online, `SyncService` dispara imediatamente; se offline, aguarda Background Sync.
     - Feedback UX: badge "sincronizado" / "pendente" / "conflito" ao lado de cada item na lista.
   - **Abordagem**: sub-form dentro do eleitor. Escolhe tipo (DOMICILIAR/PUBLICA), registra geoloc, e lista de candidatos vinculados à equipe do membro (`GET /api/equipes/{id}/candidatos`) com radio por candidato para a intenção (FAVORAVEL/INDECISO/CONTRARIO/HOSTIL). Suporta **múltiplos candidatos** na mesma abordagem (RF-14).
   - **Consentimento** dentro da abordagem — componente com 2 abas:
     - Aba 1: **Assinatura em tela** — usa `<sgce-signature-pad>` do `shared/` (skill 07). Exporta PNG base64.
     - Aba 2: **QR code wa.me** — chama `GET /api/deep-link-opt-in?abordagem_id=X&candidato_id=Y`, recebe URL + PNG data-URI, renderiza QR. Instrução na tela: "Peça ao eleitor apontar a câmera do próprio celular".
     - Toggles independentes para `consentimento_dados` e `consentimento_whatsapp_marketing` (RF-15).
   - **Revogação**: botão "revogar dados" na tela de detalhe do eleitor — dispara `POST /api/consentimentos/{id}/revogar-dados`. Ao voltar, o eleitor aparece como "Eleitor anonimizado #{id}" com PII em branco (backend fez conforme D-02).

3. **Feature `mapa`**:
   - `<sgce-mapa>` com Leaflet, tile OSM (dev).
   - **Nível geográfico automático (RF-04)**: baseado no `cargo` do `Candidato` logado (`AuthService.currentUser.cargo`):
     - PRESIDENTE → mapa nacional, agregação por estado.
     - SENADOR/DEPUTADO_FEDERAL/DEPUTADO_ESTADUAL → estado, agrega por município.
     - PREFEITO/VEREADOR → município, agrega por bairro/zona.
   - Fonte dos polígonos: `GET /api/regioes-eleitorais?nivel=ESTADO&pai_id=...` (skill 04). Renderiza como camada Leaflet.
   - Heatmap: cor da região é calculada via `GET /api/eleitores/agregado?regiao_id=X&candidato_id=Y` — retorna % FAVORAVEL. Gradiente vermelho→amarelo→verde.

4. **Feature `dashboard` + integração RealtimeService (RF-18 a RF-20)**:
   - Painel com contadores (eleitores abordados, convencidos, regiões top/bottom por intenção).
   - Mapa lateral com localização em tempo real dos membros (círculos animados, `RF-19`).
   - **`RealtimeService.activate()`** — conecta em `/ws` (nginx proxy), subscribe `/topic/tempo-real/{partidoId}`. Recebe:
     - `heartbeat_membro` → atualiza círculo do membro no mapa.
     - `abordagem_sincronizada` → recolore região correspondente (estado 2, gradient).
     - `membro_offline_coletando` → região vira cor distinta com tooltip "Equipe em campo — sem conexão" (estado 3, RF-20).
   - Sinal cinza (estado 1) é o default quando não há dado nenhum para aquela região.
   - Auto-reconecta com backoff exponencial se WebSocket cair.

5. **Toggle "modo campo" do voluntário (D-10)**:
   - No perfil do usuário (menu topo), toggle "Modo campo ativo". Quando ligado, o app começa a enviar heartbeats a cada 30s via `POST /api/tempo-real/heartbeat`. Quando desligado, para.
   - Bloqueia o toggle se `ConsentimentoMembro` não estiver ativo — link para tela de aceite do termo.

6. **PWA**:
   - `manifest.webmanifest` com nome "SGCE Campo", ícones (placeholder), display standalone.
   - `ngsw-config.json` com estratégias: `dataGroups` para APIs (freshness curto), `assetGroups` para app shell (prefetch).
   - Testar instalação (Chrome DevTools → Application → Manifest → Add to homescreen simulado).

7. **Testes**:
   - Jest:
     - `SyncService` com features novas (mock `fake-indexeddb`): cadastro offline → outbox → sync → confirmed.
     - Conflito: mock response `{status: CONFLICT_RESOLVED}` — notificador chamado.
     - `RealtimeService`: mock STOMP client, valida subscribe/dispatch.
   - Componente `<sgce-signature-pad>`: valida que gera PNG não vazio ao "desenhar" (simular touch events).
   - Componente `<sgce-mapa>`: valida escolha de nível pelo cargo (mock `AuthService`).

## Definition of Done (verificável)

```bash
cd frontend && npm test   # verde

docker-compose up --build -d
# http://localhost:4200

# Manual (crítico — este é o fluxo mais importante do sistema):
# 1. Login como MEMBRO_EQUIPE (com ConsentimentoMembro ativo)
# 2. Liga toggle "modo campo" — círculo do próprio membro aparece no dashboard de outro usuário logado em outra aba
# 3. DevTools → Network → Offline: throttling
# 4. Cadastra eleitor completo (nome, endereço, geoloc, abordagem, intenção múltipla, consentimento por assinatura)
# 5. Item aparece na lista com badge "pendente"
# 6. Sem conexão: continua conseguindo cadastrar outros eleitores
# 7. DevTools → Network → Online: SyncService drena outbox automaticamente
# 8. Badges viram "sincronizado"; se conflito, notificação aparece
# 9. Em outra aba (usuário ADMIN), dashboard reflete os novos cadastros em <5s via WebSocket
# 10. Login como CANDIDATO com cargo=PREFEITO → mapa abre no nível do município, colorindo bairros por intenção
# 11. Login como CANDIDATO com cargo=PRESIDENTE → mapa abre nacional, colorindo estados
# 12. Region sem heartbeat de membro por >2min: cor distinta + tooltip "Equipe em campo — sem conexão"
```

## Notas para skills seguintes

- Skill 09 (testes E2E) automatiza o roteiro manual acima com Playwright, usando `context.setOffline(true)` para o ciclo offline.
- Skill 10 finaliza deploy: substitui dev-server por Angular buildado atrás do nginx.
