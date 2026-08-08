---
name: sgce-01-backend-core
description: Gera o backend base do SGCE — projeto Maven Spring Boot com Java 21, arquitetura hexagonal, módulos shared (TenantContext, RLS por SET LOCAL, CORS, OpenAPI, S3 apontando MinIO), autenticacao (JWT em cookie httpOnly + CSRF), auditoria esqueleto (LogAuditoria via AOP), e teste ArchUnit travando a fronteira hexagonal desde o dia 1. Segunda skill do roadmap SGCE, roda após sgce-00-infra-base. Use quando o usuário quiser criar o backend do zero, configurar autenticação por cookie, ou preparar a fundação hexagonal antes dos módulos de negócio.
---

# sgce-01-backend-core — Backend Spring Boot: shared + auth + auditoria + ArchUnit

## Contexto

Segunda skill do roadmap. Cria o projeto Spring Boot dentro de `backend/`, a fundação hexagonal e os três módulos transversais que **todos** os outros módulos vão depender: `shared`, `autenticacao` e `auditoria`. Não implementa nenhum módulo de negócio ainda.

**Assume:** skill 00 rodou (`docker-compose up postgres redis minio` sobe).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seções 3 (estrutura back-end), 5 (autenticação e perfis), 6 (estratégia de testes)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-21, RF-22, RNF-03, RNF-04, RNF-06, RNF-13, RNF-16, RNF-17
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-05 (RLS + pool), D-08 (JWT em cookie httpOnly + CSRF), D-11 (fuso UTC), D-12 (versões estáveis)

## Passos

1. **Projeto Maven em `backend/`**: Spring Boot **3.x estável mais recente compatível com Java 21** (D-12), `pom.xml` pinado. Dependências: Spring Web, Spring Data JPA, Spring Security, Spring WebSocket, Spring AOP, driver PostgreSQL, Hibernate Spatial, springdoc-openapi-starter-webmvc-ui, Flyway (`flyway-core` + `flyway-database-postgresql`), Validation, Lombok, AWS SDK v2 (`s3`), JUnit 5, Mockito, Spring Security Test, Testcontainers (`postgresql`, `junit-jupiter`), ArchUnit (`archunit-junit5`), JaCoCo plugin.

2. **`application.yml`** com profiles `dev`, `docker`, `test`. Conexão Postgres (`jdbc:postgresql://localhost:5432/sgce` em `dev`, `postgres:5432` em `docker`), Redis, credenciais MinIO (S3 client apontando para `http://localhost:9000` em dev com path-style access). `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` (D-11).

3. **Estrutura de pacotes** conforme seção 3 do modelo técnico, sob `com.campanha`:
   ```
   com.campanha/
   ├── shared/
   │   ├── config/       (CorsConfig, OpenApiConfig, S3Config, GlobalExceptionHandler)
   │   ├── multitenancy/ (TenantContext, TenantFilter, TenantAwareTransactionAspect)
   │   └── storage/      (S3StorageAdapter — implementa ports que virão em outras skills)
   ├── autenticacao/
   │   ├── domain/       (Usuario, Perfil enum)
   │   ├── application/  (port/in, port/out, service)
   │   └── infrastructure/
   │       ├── adapter/in/web/          (AuthController: login, refresh, logout, csrf-token)
   │       ├── adapter/out/persistence/ (UsuarioJpaAdapter, UsuarioJpaEntity, UsuarioJpaRepository)
   │       └── security/                (SecurityConfig, JwtTokenProvider, JwtCookieAuthenticationFilter, CsrfCookieFilter)
   └── auditoria/
       ├── domain/       (LogAuditoria, Auditavel annotation)
       ├── application/
       └── infrastructure/
           ├── adapter/out/persistence/ (LogAuditoriaJpaAdapter)
           └── aspect/                  (AuditoriaAspect — @Around em @Auditavel)
   ```

4. **Multi-tenancy (D-05)**:
   - `TenantContext` thread-local com `partido_id` (nulo para SUPER_ADMIN).
   - `TenantFilter` (`OncePerRequestFilter`) extrai `partido_id` do JWT do cookie e popula o `TenantContext`.
   - `TenantAwareTransactionAspect`: `@Around` em todo método `@Transactional` de `application.service.*` — executa `SET LOCAL app.current_partido_id = :tenant` na conexão. **Nunca** usar `SET` (sem `LOCAL`).
   - Migration Flyway `V1__enable_extensions.sql`: `CREATE EXTENSION IF NOT EXISTS postgis;` + parâmetro `app.current_partido_id` documentado.

5. **Autenticação (D-08)**:
   - `SecurityConfig`: CSRF ativo com `CookieCsrfTokenRepository` (não httpOnly, para o Angular ler), sessions stateless, CORS com origem explícita (`http://localhost:4200` em dev) e `allowCredentials=true`.
   - `JwtTokenProvider`: HS256 com segredo de `application.yml`, access token 15min, refresh token 7 dias.
   - `AuthController`:
     - `POST /api/auth/login`: valida credenciais → seta 2 cookies (`sgce_access` e `sgce_refresh`) httpOnly+Secure(em prod)+SameSite=Lax → retorna corpo com dados do usuário (sem tokens).
     - `POST /api/auth/refresh`: lê `sgce_refresh` do cookie, gera novo access, seta cookie.
     - `POST /api/auth/logout`: expira ambos os cookies.
     - `GET /api/auth/csrf-token`: força emissão do cookie CSRF.
   - `JwtCookieAuthenticationFilter`: extrai `sgce_access` do cookie, popula `SecurityContext`.
   - Migration Flyway `V2__usuarios.sql` com tabela `usuarios` (senha via BCrypt) e seed de 1 SUPER_ADMIN sintético (`superadmin@sgce.local` / senha `changeme-in-prod` — comentário no arquivo avisando).

6. **Auditoria**:
   - `@Auditavel` annotation em métodos de use case.
   - `AuditoriaAspect` (`@Around`): captura usuário, entidade, id, `dados_antes` (serializando o input) e `dados_depois` (o retorno), grava em `logs_auditoria`.
   - Migration `V3__logs_auditoria.sql` com `dados_antes jsonb`, `dados_depois jsonb`, índices por `usuario_id`, `entidade`, `timestamp`.

7. **OpenAPI (RNF-16)**: `OpenApiConfig` habilita Swagger UI em `/swagger-ui.html`. Em profile `prod`, desabilitar via `springdoc.api-docs.enabled=false` (ou proteger — decisão do deploy final na skill 10).

8. **Teste ArchUnit** em `backend/src/test/java/com/campanha/architecture/HexagonalArchitectureTest.java`:
   - `domain` não importa `org.springframework.*` nem `jakarta.persistence.*` nem `com.fasterxml.jackson.*`.
   - `application` importa `domain` mas nunca `infrastructure`.
   - Adapters `infrastructure/adapter/in/web` não importam de outros módulos diretamente — só via `application.port.in`.

9. **Adicionar `backend` ao `docker-compose.yml`** herdando do skill 00: Dockerfile provisório em `backend/Dockerfile.dev` (build local `mvn spring-boot:run`), portas `8080:8080`, `depends_on: [postgres, redis, minio-init]`, variáveis de ambiente apontando para os serviços internos.

## Definition of Done (verificável)

```bash
# 1. Sobe tudo
docker-compose up --build -d
# 2. Backend responde
curl -f http://localhost:8080/actuator/health   # {"status":"UP"}
# 3. Swagger UI acessível
curl -sf http://localhost:8080/swagger-ui.html > /dev/null
# 4. Endpoint autenticado bloqueia sem cookie
curl -sw "%{http_code}" -o /dev/null http://localhost:8080/api/auth/me   # 401
# 5. Login funciona e seta cookie
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"superadmin@sgce.local","senha":"changeme-in-prod"}'   # 200, cookie httpOnly setado
# 6. Endpoint autenticado libera com cookie
curl -b cookies.txt -sw "%{http_code}" -o /dev/null http://localhost:8080/api/auth/me   # 200
# 7. Testes passam (inclui ArchUnit)
cd backend && ./mvnw test
```

Todos os itens acima devem passar.

## Notas para skills seguintes

- As skills 02–06 assumem que `TenantFilter` + `TenantAwareTransactionAspect` já garantem o `SET LOCAL`. Não repetir essa lógica nos módulos de negócio.
- Toda migration Flyway das próximas skills começa de `V10__*.sql` em diante (V1–V9 reservadas para o core).
- Novo módulo = novo pacote sob `com.campanha`. O ArchUnit já cobre a regra.
- Storage: use `S3StorageAdapter` (implementa ports que os módulos declararão — ex.: `ComprovanteStoragePort` em `financeiro`, `AssinaturaStoragePort` em `consentimento`).
