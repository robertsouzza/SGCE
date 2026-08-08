---
name: sgce-10-deploy-cicd
description: Finaliza o SGCE para deploy — Dockerfile multi-stage do backend (build Maven → imagem JRE 21 slim), Dockerfile do frontend (build Angular → nginx com proxy /api e /ws para o backend), docker-compose.yml final unificando tudo (Postgres+PostGIS, Redis, MinIO, backend, frontend/nginx), script scripts/import-ibge.sh para carregar a malha territorial real do IBGE, workflow GitHub Actions em dois jobs (backend com mvn verify + Testcontainers, frontend com Jest + Playwright) que bloqueia merge se algum teste falhar (RNF-20), e README completo com diagrama de arquitetura, instruções de execução e lembrete de que o financeiro complementa e não substitui a prestação de contas no TSE. Última skill do roadmap, roda após sgce-09-testes-integracao-e2e. Use quando o usuário quiser empacotar o sistema para deploy, configurar o pipeline de CI ou escrever a documentação final do projeto.
---

# sgce-10-deploy-cicd — Dockerfiles finais + docker-compose unificado + GitHub Actions + README

## Contexto

Última skill do roadmap. Fecha o projeto para ser executado com **um único comando** (`docker-compose up --build`) do zero, e blinda o pipeline de CI. Nada de código novo de negócio — só empacotamento, orquestração e documentação.

**Assume:** skills 00–09 (sistema completo e testado localmente).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — visão geral + `Pendências fora do código`
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RNF-07, RNF-16, RNF-20
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-06 (script IBGE), D-08 (nginx reverso para JWT em cookie), D-12 (versões pinadas)

## Passos

1. **`backend/Dockerfile`** multi-stage:
   ```dockerfile
   FROM maven:3.9-eclipse-temurin-21 AS build
   WORKDIR /app
   COPY pom.xml .
   RUN mvn dependency:go-offline
   COPY src ./src
   RUN mvn package -DskipTests

   FROM eclipse-temurin:21-jre-alpine
   WORKDIR /app
   COPY --from=build /app/target/*.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java","-jar","app.jar"]
   ```
   - Não copia `.m2` do host; camada de dependências fica cacheada pelo `pom.xml`.
   - Usuário não-root (adicionar `RUN adduser -D sgce && USER sgce`).

2. **`frontend/Dockerfile`** multi-stage:
   ```dockerfile
   FROM node:22-alpine AS build
   WORKDIR /app
   COPY package*.json ./
   RUN npm ci
   COPY . .
   RUN npm run build -- --configuration=production

   FROM nginx:alpine
   COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
   COPY nginx.conf /etc/nginx/conf.d/default.conf
   EXPOSE 80
   ```
   Usar versão estável mais recente do node compatível com o Angular gerado (D-12).

3. **`frontend/nginx.conf`** (produção — mesma origem, proxy transparente):
   ```
   server {
     listen 80;
     server_name _;
     root /usr/share/nginx/html;

     # Preserva cookies para o backend
     location /api {
       proxy_pass http://backend:8080;
       proxy_set_header Host $host;
       proxy_set_header Cookie $http_cookie;
       proxy_set_header X-Real-IP $remote_addr;
     }
     location /ws {
       proxy_pass http://backend:8080;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
     }
     location /swagger-ui.html { return 404; }   # produção esconde Swagger (RNF-16)
     location /v3/api-docs      { return 404; }

     # SPA fallback
     location / { try_files $uri $uri/ /index.html; }

     # Headers de segurança básicos
     add_header X-Frame-Options "DENY";
     add_header X-Content-Type-Options "nosniff";
     add_header Referrer-Policy "strict-origin-when-cross-origin";
   }
   ```

4. **`docker-compose.yml` final** — evolução do da skill 00, adicionando backend + frontend:
   - Serviços: `postgres`, `redis`, `minio`, `minio-init`, `backend`, `frontend`.
   - `backend`: build `./backend`, `depends_on: {postgres: {condition: service_healthy}, redis: ..., minio-init: {condition: service_completed_successfully}}`, healthcheck em `/actuator/health`.
   - `frontend`: build `./frontend`, `depends_on: {backend: {condition: service_healthy}}`, publica só porta 80 (usuário acessa `http://localhost/`; API e WebSocket via mesma origem).
   - Healthchecks em todos os serviços (`healthcheck:` no compose).
   - `.env.example` na raiz com placeholders (`POSTGRES_PASSWORD`, `JWT_SECRET`, `MINIO_ROOT_PASSWORD`) — `.env` já ignorado no `.gitignore` da skill 00.
   - Segredos: **não** hard-code em `docker-compose.yml`; usar `${VAR}` referenciando o `.env`.

5. **`scripts/import-ibge.sh`** (D-06):
   ```bash
   #!/usr/bin/env bash
   # Baixa malha municipal do IBGE (shapefile) e carrega em regioes_eleitorais via shp2pgsql.
   # Uso: ./scripts/import-ibge.sh [UF-CODE|BR]
   # Requer: unzip, shp2pgsql (postgis-tools), psql, curl.
   # NÃO ROTEIA ANONIMIZAÇÃO. Rodar UMA VEZ após primeiro `docker-compose up`.
   ...
   ```
   - Baixa de `https://geoftp.ibge.gov.br/organizacao_do_territorio/malhas_territoriais/malhas_municipais/municipio_2022/UFs/{UF}/{UF}_Municipios_2022.zip`.
   - `shp2pgsql -a -s 4326 -W LATIN1 ...` gera SQL de INSERT contra `regioes_eleitorais`.
   - Antes de rodar: apaga as regiões sintéticas (`DELETE FROM regioes_eleitorais WHERE codigo_ibge LIKE '%FAKE%'`).

6. **CI — `.github/workflows/ci.yml`** (RNF-20):
   ```yaml
   name: CI
   on: [push, pull_request]
   jobs:
     backend:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-java@v4
           with: { java-version: '21', distribution: 'temurin', cache: 'maven' }
         - run: cd backend && ./mvnw verify   # inclui Testcontainers (Docker-in-Docker do runner) e JaCoCo gate
     frontend:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-node@v4
           with: { node-version: '22', cache: 'npm', cache-dependency-path: frontend/package-lock.json }
         - run: cd frontend && npm ci
         - run: cd frontend && npm test                       # Jest
         - run: cd frontend && npx playwright install --with-deps
         - run: docker-compose up -d postgres redis minio minio-init backend
         - run: cd frontend && npx playwright test            # E2E
         - if: always()
           uses: actions/upload-artifact@v4
           with: { name: playwright-report, path: frontend/playwright-report/ }
   ```
   - Ambos os jobs **bloqueantes** — merge para `main` só se ambos verdes (proteção de branch do GitHub, configuração manual documentada no README).

7. **README.md final** (substitui o esqueleto da skill 00):
   - Título + 1 parágrafo do que é.
   - **Aviso destacado (topo)**: sistema real, dados sensíveis, LGPD + Resolução TSE 23.610, nunca commitar dado real.
   - Diagrama de arquitetura (ASCII do meta-skill ou Mermaid).
   - Stack técnica resumida.
   - **Como rodar**: `cp .env.example .env` → editar → `docker-compose up --build` → aguardar healthchecks → abrir `http://localhost/`.
   - URLs úteis: frontend `/`, Swagger só em dev `/swagger-ui.html`, MinIO console `:9001`.
   - Como popular malha IBGE real: `./scripts/import-ibge.sh SP` (opcional, dev funciona sem).
   - **Como rodar testes**: `cd backend && ./mvnw verify` / `cd frontend && npm test && npx playwright test`.
   - **Decisões técnicas principais** (5–7 bullets): monólito modular hexagonal, multi-tenant RLS, JWT em cookie httpOnly, offline-first Dexie+SyncService, WebSocket via Redis pub/sub, ArchUnit trava fronteira, MinIO como stub de S3.
   - **Lembrete jurídico**: o módulo financeiro **complementa e não substitui** a prestação de contas oficial no SPCE do TSE (`Pendências fora do código` do modelo técnico).
   - **Ainda a fazer antes de ir a prod**: revisão jurídica dos termos, integração real de WhatsApp (skill separada), import da malha IBGE real, hardening de segredos (Vault/secret manager), certificado TLS via Let's Encrypt no nginx.
   - Créditos, licença.

## Definition of Done (verificável)

```bash
# Do zero, em uma máquina limpa com Docker:
git clone <repo> sgce && cd sgce
cp .env.example .env
docker-compose up --build -d

# Aguarda healthchecks (2-3 min primeira vez)
docker-compose ps   # todos "healthy"

# Fluxo mínimo funcional:
open http://localhost/
# Login → cadastro de partido/candidato → equipe → eleitor com consentimento → dashboard atualiza
# ← todos os itens do DoD manual das skills 07 e 08 funcionam nesta stack

# CI local (act ou push para branch):
gh workflow run ci.yml   # ambos os jobs verdes

# Regressão intencional: comitar código que quebra teste multi-tenant → CI falha → merge bloqueado
```

## Notas finais

Após esta skill, o projeto está **funcionalmente completo** para MVP e apto a receber:

- Integração real de WhatsApp (skill futura, fora deste roadmap).
- Import da malha IBGE real (rodar `./scripts/import-ibge.sh` — não é código, é ops).
- Hardening de produção (secret manager, TLS, monitoring — fora do scaffolding).

Alterações posteriores a este ponto devem passar por skills próprias (novas features), nunca modificar os SKILL.md existentes retroativamente exceto para correção de bug.
