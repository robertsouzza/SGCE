---
name: sgce-07-frontend-core-gestao
description: Gera o frontend base do SGCE — projeto Angular estável mais recente PWA standalone components, camada core/ (AuthService com cookie httpOnly + interceptor CSRF, RealtimeService WebSocket/STOMP com stub, OfflineStore Dexie/IndexedDB + SyncService com outbox e Background Sync), shared/ (componentes genéricos, canvas de assinatura), layout shell dinâmico por perfil, e features de gestão (auth login/logout, partido, equipe, financeiro). Oitava skill do roadmap, primeira de frontend, roda após sgce-06-modulo-tempo-real-superadmin. Use quando o usuário quiser gerar o frontend do zero, implementar login por cookie ou as telas de cadastro de partido/equipe/financeiro.
---

# sgce-07-frontend-core-gestao — Angular PWA + core/ + features de gestão

## Contexto

Oitava skill (primeira de frontend). Cria o esqueleto do Angular e as camadas transversais + as features de gestão (menos "sensíveis a campo"). Deixa `eleitores`, `mapa` e `dashboard` para a skill 08.

**Assume:** skills 00–06 (backend inteiro no ar; endpoints consumidos aqui).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 2 (offline-first) + seção 4 (estrutura front-end) + seção 5 (perfis)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-01, RF-03, RF-05 a RF-11, RF-21, RNF-04, RNF-10
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-08 (JWT em cookie httpOnly + CSRF via nginx reverso), D-12 (Angular estável mais recente)

## Passos

1. **`frontend/` com `ng new frontend --standalone --routing --style=scss`** usando a versão estável **mais recente** do Angular na data da geração (D-12 — não "19" hard-coded). `ng add @angular/pwa`.

2. **Dependências**: Dexie.js, `signature_pad`, `@stomp/rxjs`, `zone.js` (se não já vier), `qrcode` (para o QR code do consentimento no skill 08 já usar). Não instalar Leaflet/Mapbox ainda — isso é da skill 08.

3. **Estrutura de pastas** conforme seção 4 do modelo técnico:
   ```
   src/app/
   ├── core/
   │   ├── auth/          (AuthService, AuthGuard, RoleGuard, CsrfInterceptor)
   │   ├── realtime/      (RealtimeService — stub que a skill 08 ativará)
   │   ├── offline/       (OfflineStore Dexie, SyncService com outbox + Background Sync)
   │   └── layout/        (Shell, navbar/sidebar por perfil)
   ├── shared/            (componentes genéricos: table, card, modal, form-field, signature-pad wrapper)
   ├── features/
   │   ├── auth/          (login, recuperação de senha — placeholder)
   │   ├── partido/       (cadastro/listagem)
   │   ├── equipe/        (líder, membro, EquipeCandidato)
   │   └── financeiro/    (recursos, despesas, aprovações, relatório)
   ├── state/             (services com RxJS/Signals)
   └── app.routes.ts
   ```

4. **AuthService + interceptors (D-08)**:
   - `AuthService`: `login(email, senha)` → `POST /api/auth/login` com `withCredentials: true`. Cookie httpOnly volta transparente; frontend nunca lê o token. Estado `currentUser$` (Signal ou BehaviorSubject) populado por `GET /api/auth/me` após login.
   - `CsrfInterceptor`: em toda mutação (`POST`/`PUT`/`PATCH`/`DELETE`), lê o valor do cookie `XSRF-TOKEN` (visível ao JS pois **não é** httpOnly) e injeta como header `X-XSRF-TOKEN`. Bootstrap chama `GET /api/auth/csrf-token` uma vez ao iniciar app para forçar emissão.
   - `WithCredentialsInterceptor`: adiciona `withCredentials: true` a toda request para o backend.
   - `AuthGuard`: redireciona para `/login` se `currentUser$` está vazio; se resposta é 401 num interceptor, dispara auto-refresh (`POST /api/auth/refresh`) e retry — se refresh falhar, força logout.
   - `RoleGuard`: parametrizado por perfis permitidos.

5. **OfflineStore + SyncService** (base — skill 08 vai adicionar as features de campo que **usam** este core):
   - `OfflineStore` (Dexie): schema com tabelas `outbox` (operações pendentes: `id (autoincrement)`, `client_op_id (uuid)`, `entidade`, `operacao`, `payload`, `timestamp_local`, `status: PENDING|SENT|CONFIRMED|ERROR|CONFLICT`), `eleitores_locais`, `abordagens_locais`, `consentimentos_locais` (tabelas por entidade que o campo cria offline). Ver skill 08 para uso real.
   - `SyncService`: enfileira em `outbox`, tenta enviar imediatamente; se offline (validação real via ping `HEAD /api/actuator/health`, não só `navigator.onLine`), agenda via **Background Sync API**. Ao reconectar, drena a outbox em ordem, batch de até 50 operações por request, chamando `POST /api/sincronizacao/lote` (skill 04). Trata response por operação: `CONFIRMED`, `CONFLICT` (mostra notificação ao usuário via serviço `NotifierService`), `CLOCK_SKEW` (para o sync e alerta usuário).
   - `RealtimeService` (stub por enquanto): expõe `Observable<TempoRealEvento>` vazio. Skill 08 conecta ao STOMP.

6. **Layout Shell + nav dinâmica por perfil**:
   - `<sgce-shell>` com sidebar/topbar. `layoutService.menuPorPerfil(user.perfil)` retorna a lista de itens visíveis.
   - Perfis: `SUPER_ADMIN`, `ADMIN`, `CANDIDATO`, `GERENTE_FINANCEIRO`, `SECRETARIO`, `LIDER_EQUIPE`, `MEMBRO_EQUIPE`. Cada um vê só o que tem permissão (matriz do modelo técnico).

7. **Features**:
   - `auth/login`: form com email/senha, chama `AuthService.login()`, redireciona para `/dashboard` (placeholder por enquanto — skill 08 implementa dashboard real).
   - `partido/`: listar (só SUPER_ADMIN), criar, editar. Cadastro de `Candidato` com validação client-side de `municipio` obrigatório para PREFEITO/VEREADOR (o backend valida também, D-05).
   - `equipe/`: listar, criar equipe, adicionar membros, vincular candidatos (`EquipeCandidato`).
   - `financeiro/`:
     - Recursos: lista + criar.
     - Despesas: lista, criar (upload de comprovante via multipart), fluxo `PENDENTE → APROVADO/REJEITADO` (botões só aparecem para `GERENTE_FINANCEIRO`).
     - Relatório: botão "Baixar PDF" e visualização JSON agregada.

8. **Nginx reverso** — atualiza `docker-compose.yml`: adiciona serviço `frontend` (imagem `nginx:alpine` para dev, dev-server local para hot reload — decisão de deploy final na skill 10). Config nginx:
   ```
   server {
     location /api { proxy_pass http://backend:8080; proxy_set_header Cookie $http_cookie; }
     location /ws  { proxy_pass http://backend:8080; proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; }
     location /    { root /usr/share/nginx/html; try_files $uri /index.html; }
   }
   ```
   Mesma origem → cookie httpOnly funciona sem drama cross-site.

9. **Testes**:
   - Jest (unit): `AuthService` (mock HttpClient), `CsrfInterceptor` (verifica header injetado), `SyncService` (mock IndexedDB via `fake-indexeddb`, cenários: offline enfileira → online drena; conflito notifica).
   - Componentes: form de login, form de despesa, canvas de assinatura básico.
   - **Sem E2E ainda** — skill 09 cobre.

## Definition of Done (verificável)

```bash
cd frontend && npm test   # Jest verde

docker-compose up --build -d
# Abre http://localhost:4200 (ou porta configurada) no navegador

# Manual:
# 1. Tela de login aparece
# 2. Login com superadmin@sgce.local funciona; cookie httpOnly setado (DevTools → Application → Cookies)
# 3. Sidebar mostra opções condizentes com SUPER_ADMIN
# 4. Cadastra partido, cadastra candidato PREFEITO sem município → erro client-side e server-side
# 5. Cria usuário ADMIN, faz login como ele
# 6. Sidebar muda (não vê a opção "Partidos" — só SUPER_ADMIN vê)
# 7. Cria equipe, adiciona membro, vincula candidato
# 8. Cria recurso, cria despesa com upload de PDF → visível na lista PENDENTE
# 9. Login como GERENTE_FINANCEIRO → aprova despesa → status muda para APROVADO
# 10. Botão "Baixar relatório PDF" → arquivo baixa
# 11. Logout limpa cookies, redireciona para /login
```

## Notas para skills seguintes

- Skill 08 adiciona features `eleitores`, `mapa`, `dashboard`, e ativa o `RealtimeService` (que hoje é stub).
- Skill 08 vai USAR o `SyncService`/`OfflineStore` — não recriar. Só adicionar novas tabelas no schema Dexie via `db.version(N).stores(...)`.
- Skill 09 (E2E) inclui Playwright cobrindo os fluxos manuais acima.
