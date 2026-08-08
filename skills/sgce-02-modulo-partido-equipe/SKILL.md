---
name: sgce-02-modulo-partido-equipe
description: Gera os módulos partido (Partido, Candidato com validação de cargo/UF/município no domínio) e equipe (Equipe, MembroEquipe, EquipeCandidato N:N) do backend SGCE, seguindo arquitetura hexagonal. Inclui migrations Flyway com Row-Level Security por partido_id e o teste de isolamento multi-tenant. Terceira skill do roadmap, roda após sgce-01-backend-core. Use quando o usuário quiser cadastrar partidos e candidatos, montar equipes de campanha ou testar que o RLS multi-tenant funciona na prática.
---

# sgce-02-modulo-partido-equipe — Partido, Candidato, Equipe, MembroEquipe

## Contexto

Terceira skill. Primeiros módulos de negócio. Junta `partido` e `equipe` numa skill só porque `EquipeCandidato` é N:N com `Candidato` — separar criaria acoplamento chato entre skills.

**Assume:** skills 00 e 01 (backend rodando, `TenantContext`, RLS via `SET LOCAL`, `autenticacao`, `auditoria` prontos).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — seção 1 (Partido, Candidato, Equipe, MembroEquipe, EquipeCandidato) e seção 3 (estrutura de pacotes)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-01, RF-02, RF-03, RF-10, RF-11, RNF-06, RNF-18
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-05 (RLS + pool)

## Passos

1. **Migrations Flyway** (`V10__partidos.sql`, `V11__candidatos.sql`, `V12__equipes.sql`):
   - Todas as tabelas com `partido_id BIGINT NOT NULL` (exceto `partidos` que é o próprio tenant).
   - `ALTER TABLE ... ENABLE ROW LEVEL SECURITY;` em cada uma.
   - Policy `tenant_isolation`: `USING (partido_id = current_setting('app.current_partido_id')::bigint)` — aplica-se a `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
   - Para `partidos`: policy usa `id = current_setting(...)::bigint`, exceto quando `current_setting('app.current_partido_id', true) IS NULL` (caso do SUPER_ADMIN) — permite listar todos.
   - Índices em `(partido_id, created_at)` nas de negócio.

2. **Módulo `partido`** (`com.campanha.partido`):
   - **Domain:** `Partido` (dados cadastrais), `Candidato` com enum `Cargo` (PRESIDENTE, SENADOR, DEPUTADO_FEDERAL, DEPUTADO_ESTADUAL, PREFEITO, VEREADOR). Método `Candidato.validar()` que lança `MunicipioObrigatorioException` quando `cargo ∈ {PREFEITO, VEREADOR}` e `municipio == null`. **Regra no domínio, não só na validação de DTO.**
   - **Application:** ports `PartidoRepositoryPort`, `CandidatoRepositoryPort`, use cases `CadastrarPartidoUseCase`, `AtualizarPartidoUseCase`, `ListarPartidosUseCase`, `CadastrarCandidatoUseCase`, `ListarCandidatosUseCase`. Métodos de use case anotados com `@Auditavel` e `@Transactional`.
   - **Infrastructure:**
     - `PartidoController` e `CandidatoController` em `adapter/in/web/`. Endpoints com validação de permissão por perfil (só `SUPER_ADMIN` cadastra partido; `ADMIN` do partido cadastra candidatos).
     - `PartidoJpaAdapter`, `CandidatoJpaAdapter` em `adapter/out/persistence/`.

3. **Módulo `equipe`** (`com.campanha.equipe`) — mesmo padrão:
   - **Domain:** `Equipe`, `MembroEquipe`, `EquipeCandidato` (VO com `vigente_desde`/`vigente_ate`).
   - **Application:** `CadastrarEquipeUseCase`, `AdicionarMembroUseCase`, `VincularCandidatoAEquipeUseCase`, `ListarEquipesPorPartidoUseCase`.
   - **Infrastructure:** controllers e JPA adapters.

4. **Permissões (RF-21)** — reforçar no controller além do RoleGuard futuro do front:
   - Cadastro/edição de `Partido`: só `SUPER_ADMIN_PLATAFORMA`.
   - Cadastro/edição de `Candidato`: `ADMIN` do partido.
   - Cadastro de `Equipe`/`MembroEquipe`: `ADMIN` ou `LIDER_EQUIPE` (líder só edita a própria equipe).
   - `EquipeCandidato`: `ADMIN`.

5. **Testes**:
   - Unit (`domain/`): validação de `MunicipioObrigatorioException` em todos os cenários de cargo.
   - Application (Mockito): use cases orquestram corretamente as ports.
   - Integration (`@SpringBootTest` + `@Testcontainers`): CRUD end-to-end contra Postgres+PostGIS real. Neste ponto **já valida RLS na prática** — o teste faz login como usuário do partido A e tenta ler partido B: `SELECT` retorna vazio.
   - Contract (`@WebMvcTest` ou `MockMvc`): matriz de permissões — cada perfil deve receber 403 nos endpoints proibidos e 200 nos permitidos.

## Definition of Done (verificável)

```bash
cd backend && ./mvnw test
# Todos os testes verdes, incluindo:
# - HexagonalArchitectureTest (do skill 01)
# - PartidoDomainTest, CandidatoDomainTest (validação de município)
# - MultiTenantIsolationIntegrationTest (partido A não vê dado de partido B)
# - MatrizPermissoesIntegrationTest (403/200 por perfil)

# Manual (com backend up via docker-compose):
# 1. Login como SUPER_ADMIN
# 2. POST /api/partidos → cria partido 1
# 3. POST /api/partidos → cria partido 2
# 4. Cria usuário ADMIN em cada partido
# 5. Login como ADMIN do partido 1
# 6. GET /api/candidatos → retorna só candidatos do partido 1 (mesmo com dados do partido 2 no BD)
# 7. POST /api/candidatos com cargo=PREFEITO sem municipio → 400 com mensagem clara
# 8. POST /api/candidatos com cargo=PREFEITO e municipio → 201
```

## Notas para skills seguintes

- Skill 03 (`financeiro`) usa `Candidato` como FK — importa `com.campanha.partido.domain.Candidato` **apenas via ID** (Long), nunca a entidade JPA direta.
- Skill 06 (equipe em campo) usa `MembroEquipe` da mesma forma — só o ID.
- Todas as skills posteriores herdam automaticamente a RLS deste padrão — replicar `ENABLE ROW LEVEL SECURITY` + policy `tenant_isolation` em toda nova tabela.
