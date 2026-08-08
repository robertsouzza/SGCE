---
name: sgce-00-infra-base
description: Cria a base do monorepo SGCE — estrutura de pastas (backend/, frontend/), .gitignore, README esqueleto e docker-compose.yml só com a infraestrutura (PostgreSQL+PostGIS, Redis, MinIO). Primeira skill do roadmap SGCE, deve rodar antes de qualquer outra. Use quando o usuário quiser começar o projeto do zero, subir só a infraestrutura para desenvolvimento, ou preparar o terreno antes de gerar backend/frontend.
---

# sgce-00-infra-base — Monorepo + infraestrutura base

## Contexto

Primeira skill do roadmap SGCE. Cria o esqueleto do monorepo e sobe apenas os serviços de infraestrutura em Docker, sem código de aplicação. Serve para validar que o ambiente local do usuário está funcional antes de gerar código.

**Assume:** nada. É a primeira skill do roadmap.

## Referências obrigatórias

Leia antes de agir:
- `../sgce-fullstack/SKILL.md` — visão geral e regras que valem para todas as skills
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seções 1 (modelo de dados) e 3 (estrutura backend)
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-02b (storage MinIO/S3), D-05 (RLS + pool), D-06 (malha IBGE)

## Passos

1. **Estrutura de pastas na raiz do projeto:**
   ```
   backend/       (vazio por enquanto — skill 01 preenche)
   frontend/      (vazio por enquanto — skill 07 preenche)
   scripts/       (vazio — skill 10 adiciona import-ibge.sh)
   docker-compose.yml
   .gitignore
   README.md
   ```

2. **`.gitignore`** cobrindo: `target/`, `node_modules/`, `dist/`, `.env`, `*.log`, `.idea/`, `.vscode/`, IDE e OS junk. **Não** ignorar `docker-compose.yml` nem arquivos de seed sintéticos.

3. **`docker-compose.yml` inicial** com 3 serviços (nada de backend/frontend ainda):
   - **postgres**: imagem `postgis/postgis:16-3.4` (ou versão estável mais recente compatível), variáveis `POSTGRES_USER=sgce`, `POSTGRES_PASSWORD=sgce_dev` (só dev), `POSTGRES_DB=sgce`, volume nomeado `sgce_pgdata`, porta `5432:5432`.
   - **redis**: imagem `redis:7-alpine`, porta `6379:6379`, sem volume (state efêmero em dev).
   - **minio**: imagem `minio/minio:latest`, comando `server /data --console-address ":9001"`, variáveis `MINIO_ROOT_USER=minioadmin`, `MINIO_ROOT_PASSWORD=minioadmin` (só dev), portas `9000:9000` (API S3) e `9001:9001` (console), volume nomeado `sgce_miniodata`.
   - **minio-init**: container efêmero (`minio/mc:latest`) que aguarda o MinIO subir, cria o bucket `sgce-uploads` e configura política privada. Roda com `depends_on: minio` e sai.

4. **README.md esqueleto** na raiz:
   - Título e 1 parágrafo do que é o SGCE (referenciar o SKILL.md do meta).
   - **Aviso destacado**: sistema real, cuidado com dados sensíveis, nunca commitar dados reais de eleitor.
   - Seção "Requisitos": Docker + Docker Compose, ~4GB RAM livre.
   - Seção "Começar": `docker-compose up postgres redis minio minio-init`, com o que esperar como saída.
   - Seção "Estado atual": lista quais partes já foram geradas (nesta skill: só infra). Skills seguintes atualizam essa seção.

## Definition of Done (verificável)

Executando na raiz do projeto:

```bash
docker-compose up -d postgres redis minio minio-init
docker-compose ps
```

Deve mostrar:
- `postgres` com status `healthy` (ou `Up`); `docker exec -it <postgres> psql -U sgce -d sgce -c "SELECT PostGIS_Version();"` retorna a versão do PostGIS.
- `redis` com status `Up`; `docker exec -it <redis> redis-cli ping` retorna `PONG`.
- `minio` com status `Up`; `curl -f http://localhost:9000/minio/health/live` retorna 200.
- `minio-init` com status `Exited (0)`; console em http://localhost:9001 mostra o bucket `sgce-uploads` criado.

`docker-compose down` para tudo sem erro.

## Notas para skills seguintes

- Skill 01 vai adicionar o serviço `backend` neste mesmo `docker-compose.yml`, com `depends_on: [postgres, redis, minio-init]`.
- Skill 07/10 vai adicionar o serviço `frontend` (nginx com Angular buildado + proxy `/api` para o backend).
- Não crie ainda usuários no Postgres além do owner — o Flyway do backend faz isso.
