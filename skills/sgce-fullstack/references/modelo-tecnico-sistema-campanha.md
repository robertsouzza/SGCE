# Sistema de Gestão de Campanha Eleitoral 2026 — Modelo Técnico (v2)

Documento de referência para geração da skill do projeto. Consolida todas as decisões tomadas até aqui.

> **Leia também `decisoes-tomadas.md` antes de gerar código.** Ele fecha ambiguidades que restaram deste documento (fluxo do QR de consentimento, retenção pós-revogação, JWT em cookie, break-glass dual-control, RLS + pool, storage via MinIO/S3, entre outras). Em caso de conflito, `decisoes-tomadas.md` vence — foi decidido depois.

## Escopo e modelo de negócio

- **Multi-tenant por Partido**: o sistema atende múltiplos partidos, cada um com seus candidatos, equipes, financeiro e eleitores **totalmente isolados** dos demais. Nenhum partido acessa dado de outro.
- **Fora de escopo, por risco legal**: cruzar/vender a um partido a intenção de voto ou base de eleitores capturada por outro partido. A Resolução TSE 23.610 proíbe transferência/doação/venda de banco de dados eleitoral entre candidatos/partidos, e isso violaria a finalidade do consentimento LGPD dado pelo eleitor (dado sensível — opinião política). Monetização multi-partido deve ser por assinatura (SaaS), não por venda de dado cruzado. Se houver interesse futuro em "inteligência de mercado" sobre adversários, o caminho legal é revender acesso a pesquisas eleitorais oficialmente registradas no TSE — dado público, não a base privada de canvassing de terceiros.

## Stack decidida

- **Back-end**: Java 21 + Spring Boot, arquitetura hexagonal (ports & adapters) dentro de um monólito modular, princípios SOLID.
- **Banco de dados**: PostgreSQL + extensão PostGIS. Isolamento multi-tenant via `partido_id` em todas as tabelas de negócio + Row-Level Security no Postgres (cada query já filtra pelo tenant automaticamente, reduz risco de vazamento por bug de aplicação).
- **Tempo real**: WebSocket + Redis Pub/Sub.
- **Infra**: Docker, GitHub + GitHub Actions (CI/CD).
- **Front-end**: Angular 19, responsivo/PWA, Mapbox GL JS ou Leaflet para mapa de calor, com camada offline-first (IndexedDB + Background Sync).

## 1. Modelo de dados

### Módulo Tenant / Partido

**Partido**
id, nome, sigla, numero_partido, cnpj, endereco_sede, dados_bancarios_conta_partidaria, email, telefone, plano_assinatura, ativo

Todas as entidades abaixo (Candidato, Equipe, Eleitor, Despesa, etc.) carregam `partido_id` direto ou indiretamente (via Candidato/Equipe), para isolamento multi-tenant.

### Módulo Usuário / Autenticação

**Usuario**
id, partido_id (FK; **nulo** para o perfil SUPER_ADMIN_PLATAFORMA, que atua acima de qualquer partido), nome, email, senha_hash, telefone, perfil (SUPER_ADMIN_PLATAFORMA, ADMIN, CANDIDATO, GERENTE_FINANCEIRO, SECRETARIO, LIDER_EQUIPE, MEMBRO_EQUIPE), ativo, criado_em

### Módulo Financeiro

**Candidato**
id, partido_id (FK), usuario_id (FK, opcional), nome_completo, titulo_eleitor, numero_candidato, **cargo** (enum: PRESIDENTE, SENADOR, DEPUTADO_FEDERAL, DEPUTADO_ESTADUAL, PREFEITO, VEREADOR), **uf**, **municipio** (obrigatório quando cargo = PREFEITO ou VEREADOR)

**RecursoFundoEleitoral**
id, candidato_id (FK), tipo_recurso (FUNDO_ELEITORAL, FUNDO_PARTIDARIO, DOACAO), valor, data_repasse, origem, numero_documento, comprovante_url

**Despesa**
id, candidato_id (FK), categoria (PESSOAL, ALIMENTACAO, TRANSPORTE, MATERIAL_GRAFICO, OUTROS), subcategoria_tse, valor, data, descricao, lancado_por (FK Usuario), comprovante_url, status (PENDENTE, APROVADO, REJEITADO), aprovado_por (FK Usuario), aprovado_em

**PagamentoEquipe** (especialização 1—1 de Despesa quando categoria = pessoal)
id, despesa_id (FK), membro_id (FK Usuario), tipo_pagamento (DIARIA, SALARIO, POR_ABORDAGEM, POR_VISITA), quantidade, valor_unitario, periodo_referencia

### Módulo Equipe

**Equipe**
id, partido_id (FK), nome, lider_id (FK Usuario), regiao_atuacao

**MembroEquipe**
id, usuario_id (FK), equipe_id (FK), funcao, ativo

**EquipeCandidato** (N:N — quais candidatos essa equipe está autorizada a promover/perguntar, conforme orientação do coordenador do partido; pode ter 1 ou vários candidatos, cobrindo tanto abordagem para 1 candidato quanto para a chapa toda)
equipe_id (FK), candidato_id (FK), vigente_desde, vigente_ate

### Módulo Eleitores

**Eleitor**
id, partido_id (FK — o partido/campanha que primeiro cadastrou), nome_completo, endereco, geolocalizacao (PostGIS Point), telefone_whatsapp, titulo_eleitor, zona_eleitoral, secao_eleitoral, observacoes

**Abordagem** (o evento de contato — uma visita/interação)
id, eleitor_id (FK), membro_id (FK Usuario), equipe_id (FK), tipo_abordagem (DOMICILIAR, PUBLICA), data_hora, geolocalizacao_abordagem (PostGIS Point), sincronizado (bool — false enquanto só existe localmente no dispositivo)

**IntencaoVoto** (N:N entre Abordagem e Candidato — permite registrar intenção para 1 candidato específico ou para vários da chapa na mesma visita)
id, abordagem_id (FK), candidato_id (FK), intencao (FAVORAVEL, INDECISO, CONTRARIO, HOSTIL)

**RegiaoEleitoral** (hierárquica, para o mapa de calor abranger o nível certo por cargo)
id, nivel (PAIS, ESTADO, MUNICIPIO, BAIRRO_ZONA), regiao_pai_id (auto-FK), codigo_ibge, geometria (PostGIS Polygon, importado da malha territorial do IBGE), nome_regiao

O dashboard escolhe o nível de agregação automaticamente pelo `cargo` do candidato logado: PRESIDENTE → mapa nacional (agrega por estado); SENADOR/DEPUTADO_FEDERAL/DEPUTADO_ESTADUAL → mapa do estado, agregando por município (usa `uf` do Candidato); PREFEITO/VEREADOR → mapa do município, agregando por bairro/zona (usa `municipio` do Candidato).

### Módulo Consentimento LGPD

**TermoConsentimento** (versionado — se o texto mudar, sabemos exatamente o que cada eleitor concordou)
id, partido_id (FK), versao, texto, vigente_a_partir, vigente_ate

**ConsentimentoLGPD**
id, eleitor_id (FK), abordagem_id (FK), termo_versao_id (FK), metodo_captura (ASSINATURA_TELA, QRCODE_PROPRIO_CELULAR), assinatura_arquivo_url (quando aplicável), membro_captura_id (FK Usuario), geolocalizacao (PostGIS Point), timestamp_local (hora do dispositivo, mesmo offline), timestamp_sincronizacao,
**consentimento_dados** (bool, timestamp, revogado, revogado_em) — autoriza tratamento dos dados cadastrais/intenção de voto,
**consentimento_whatsapp_marketing** (bool, timestamp, revogado, revogado_em) — autoriza especificamente o recebimento de propostas, agenda e conteúdo de campanha via WhatsApp,
contato_salvo_confirmado (bool, inferido — ex: eleitor respondeu a uma mensagem, confirmando que salvou o número)

Os dois consentimentos são independentes (LGPD art. 8º §4º exige consentimento específico por finalidade): o eleitor pode ficar cadastrado e permitir a pesquisa de intenção sem necessariamente aceitar receber mensagens, e pode revogar cada um separadamente (ex: responder "SAIR" só cancela `consentimento_whatsapp_marketing`, sem apagar o cadastro).

**Fluxo de captura**: com sinal, o app oferece assinatura em tela (offline-safe) ou QR code apontado pelo próprio celular do eleitor (mais forte juridicamente, e já cobre o `consentimento_whatsapp_marketing` exigido pela Res. TSE 23.610). Sem sinal, usa só assinatura em tela — nunca bloqueia a coleta. Ao reconectar, o sistema envia uma confirmação via WhatsApp ao eleitor (reforça a robustez do consentimento colhido offline, dá chance de revogação, e sugere que ele salve o contato do candidato — sem isso, a lista de transmissão do WhatsApp não entrega, mesmo com consentimento).

### Módulo Tempo Real

**LocalizacaoEquipeTempoReal**
membro_id (FK), geolocalizacao (PostGIS Point), timestamp, status_conexao (ONLINE, OFFLINE_COLETANDO) — fonte de verdade é Redis; Postgres guarda histórico se necessário

### Módulo Auditoria

**LogAuditoria**
id, usuario_id (FK), acao, entidade, entidade_id, dados_antes (jsonb), dados_depois (jsonb), timestamp, ip

**AcessoSuporteLog** (acesso excepcional "break-glass" do Super Administrador da Plataforma aos dados de um partido específico — para suporte, manutenção ou correção de erro)
id, usuario_id (FK, deve ser SUPER_ADMIN_PLATAFORMA), partido_id_acessado (FK), motivo, escopo_acesso (módulo/entidade específica consultada), iniciado_em, expira_em, finalizado_em

O Super Administrador da Plataforma tem, por padrão, visão apenas de **métricas operacionais agregadas** de todos os partidos (uso, erros, uptime, fila de sincronização) — sem tocar no conteúdo sensível (quem disse que vota em quem). Acesso ao conteúdo de um partido específico só ocorre em "modo suporte": exige motivo registrado, é auditado em `AcessoSuporteLog` e expira automaticamente. Isso está descrito em contrato com cada partido (cláusula de operador de dados) e nunca deve ser usado para consolidar inteligência cruzada entre partidos — o problema de finalidade e de venda de banco de dados eleitoral discutido antes não se resolve só por ser a mesma pessoa/empresa acessando; ele depende do uso que se faz do dado.

### Relacionamentos principais

- Partido 1—N Candidato · Partido 1—N Equipe · Partido 1—N Usuario
- Candidato 1—N RecursoFundoEleitoral · Candidato 1—N Despesa
- Despesa 1—1 PagamentoEquipe (quando categoria = pessoal)
- Equipe 1—N MembroEquipe · Equipe N—1 Usuario (líder) · Equipe N—N Candidato (via EquipeCandidato)
- MembroEquipe 1—N Abordagem · Abordagem N—1 Eleitor · Abordagem N—N Candidato (via IntencaoVoto)
- Eleitor N—1 RegiaoEleitoral (calculado via geolocalização) · Eleitor 1—N ConsentimentoLGPD
- RegiaoEleitoral 1—N RegiaoEleitoral (auto-hierarquia PAIS > ESTADO > MUNICIPIO > BAIRRO_ZONA)
- Usuario 1—N LogAuditoria

## 2. Arquitetura offline-first (front-end de campo)

- **Escrita local primeiro**: toda Abordagem, Eleitor, IntencaoVoto e ConsentimentoLGPD é gravada primeiro no IndexedDB do dispositivo (ex: via Dexie.js), nunca depende da rede para o membro continuar trabalhando.
- **Detecção de conectividade real**: `navigator.onLine` não é confiável sozinho; o app faz um ping leve ao backend para confirmar que há internet de fato antes de tentar sincronizar.
- **Sincronização**: fila local (outbox) + Background Sync API do navegador — ao reconectar, reenvia tudo automaticamente, em ordem, com upsert por chave natural (título de eleitor) para evitar duplicidade quando dois membros cadastram o mesmo eleitor offline.
- **Sinalização no mapa (3 estados por região)**: (1) sem dados = cinza neutro; (2) intenção calculada = gradiente vermelho/amarelo/verde; (3) **"Equipe em campo — sem conexão"** = cor distinta com tooltip, acionada quando o último heartbeat de localização de um membro dentro daquela região parou de chegar mas ele estava em modo campo ativo. Ao sincronizar, a região recalcula e a cor real substitui o estado de "coletando".

## 3. Estrutura de back-end (Java 21 + Spring Boot, hexagonal)

Cada módulo de negócio (`partido`, `financeiro`, `equipe`, `eleitores`, `consentimento`, `tempo-real`, `autenticacao`, `auditoria`) segue a mesma divisão em três camadas: `domain` (regra de negócio pura, sem dependência de Spring/JPA/nada de infraestrutura), `application` (casos de uso e as portas — interfaces — que o domínio precisa) e `infrastructure` (os adaptadores concretos: controller REST, JPA/PostGIS, Redis, WhatsApp).

```
com.campanha/
├── partido/
│   ├── domain/                      Partido, Candidato (regras: ex. município obrigatório se cargo = PREFEITO/VEREADOR)
│   ├── application/
│   │   ├── port/in/                 CadastrarPartidoUseCase, CadastrarCandidatoUseCase
│   │   ├── port/out/                PartidoRepositoryPort, CandidatoRepositoryPort
│   │   └── service/                 implementação dos casos de uso
│   └── infrastructure/
│       ├── adapter/in/web/          PartidoController, CandidatoController
│       └── adapter/out/persistence/ PartidoJpaAdapter, CandidatoJpaAdapter
│
├── financeiro/
│   ├── domain/                      Despesa, RecursoFundoEleitoral, PagamentoEquipe + exceções (ex: SaldoInsuficienteException)
│   ├── application/
│   │   ├── port/in/                 LancarDespesaUseCase, AprovarDespesaUseCase, RegistrarRecursoUseCase, GerarRelatorioUseCase
│   │   ├── port/out/                DespesaRepositoryPort, RecursoRepositoryPort, ComprovanteStoragePort
│   │   └── service/
│   └── infrastructure/
│       ├── adapter/in/web/          DespesaController, RecursoController, RelatorioController (PDF/JSON)
│       ├── adapter/out/persistence/ DespesaJpaAdapter, RecursoJpaAdapter
│       └── adapter/out/storage/     ComprovanteStorageAdapter
│
├── equipe/                          (mesmo padrão — Equipe, MembroEquipe, EquipeCandidato)
│
├── eleitores/
│   ├── domain/                      Eleitor, Abordagem, IntencaoVoto, RegiaoEleitoral
│   ├── application/
│   │   ├── port/in/                 CadastrarEleitorUseCase, RegistrarAbordagemUseCase, RegistrarIntencaoVotoUseCase, SincronizarLoteOfflineUseCase
│   │   ├── port/out/                EleitorRepositoryPort, RegiaoGeoPort (consultas PostGIS)
│   │   └── service/
│   └── infrastructure/
│       ├── adapter/in/web/          EleitorController, AbordagemController, SincronizacaoController (recebe lote do offline)
│       └── adapter/out/persistence/ EleitorJpaAdapter (Hibernate Spatial/PostGIS)
│
├── consentimento/
│   ├── domain/                      TermoConsentimento, ConsentimentoLGPD
│   ├── application/                 CapturarConsentimentoUseCase, RevogarConsentimentoUseCase
│   └── infrastructure/              ConsentimentoController, adapter de armazenamento da assinatura, WhatsAppOptInAdapter
│
├── tempo-real/
│   ├── domain/                      LocalizacaoEquipe (value object)
│   ├── application/
│   │   ├── port/in/                 AtualizarLocalizacaoUseCase
│   │   └── port/out/                LocalizacaoPublisherPort (publica no Redis)
│   └── infrastructure/
│       ├── adapter/in/web/          LocalizacaoController (recebe heartbeat)
│       ├── adapter/out/messaging/   RedisPublisherAdapter
│       └── adapter/in/websocket/    DashboardWebSocketHandler (assina Redis, empurra ao cliente via STOMP)
│
├── autenticacao/
│   ├── domain/                      Usuario, Perfil
│   ├── application/                 LoginUseCase, RefreshTokenUseCase
│   └── infrastructure/              SecurityConfig, JwtTokenProvider, UsuarioJpaAdapter
│
├── auditoria/
│   ├── domain/                      LogAuditoria
│   └── infrastructure/              AuditoriaAspect (AOP — intercepta métodos anotados e grava log automaticamente)
│
└── shared/
    ├── config/                      CORS, OpenAPI/Swagger, tratamento global de exceção (@ControllerAdvice)
    └── multitenancy/                TenantContext (thread-local com o partido_id da requisição atual) + filtro que aplica `SET app.current_partido_id` no PostgreSQL a cada requisição, para a Row-Level Security valer de fato
```

**Regra de dependência**: `domain` nunca importa `infrastructure` nem bibliotecas de framework — só `application` conhece as portas, e só `infrastructure` implementa. Vale adicionar um teste de arquitetura (ArchUnit) no CI/CD que quebra o build se alguém importar `javax.persistence`/Spring dentro de `domain` — sem isso, a separação hexagonal tende a se corroer com o tempo conforme mais gente contribui com o código. Como cada módulo já nasce com essas fronteiras isoladas, extrair um deles depois para um serviço próprio (ex: `tempo-real`, se o volume de geolocalização justificar) não exige reescrever a regra de negócio — só troca o adaptador de transporte.

**Documentação e testes de endpoint**: cada módulo expõe sua documentação via `springdoc-openapi`, que gera Swagger UI automaticamente a partir dos controllers — usado durante o desenvolvimento para testar e validar endpoints sem depender de Postman. Em produção, o Swagger UI fica desabilitado ou protegido por autenticação, para não expor publicamente o mapa da API.

## 4. Estrutura de front-end (Angular 19)

```
src/app/
├── core/
│   ├── auth/            AuthService, interceptor JWT, AuthGuard, RoleGuard
│   ├── realtime/        RealtimeService (WebSocket/STOMP) — expõe Observables
│   ├── offline/         OfflineStore (IndexedDB/Dexie), SyncService (outbox + Background Sync)
│   └── layout/          Shell, navbar/sidebar dinâmicos por perfil
├── shared/               componentes genéricos (tabela, card, modal, mapa base, canvas de assinatura)
├── features/
│   ├── auth/             login, recuperação de senha
│   ├── dashboard/        visão tempo real (varia por perfil e nível geográfico do cargo)
│   ├── financeiro/       recursos, despesas, aprovações, relatórios
│   ├── equipe/           cadastro de líderes/membros, EquipeCandidato, pagamentos
│   ├── partido/          cadastro de partido e candidatos (cargo, uf, município)
│   ├── eleitores/        cadastro, abordagem, intenção de voto (1 ou vários candidatos), consentimento LGPD
│   └── mapa/             mapa ao vivo + heatmap hierárquico (país/estado/município/bairro)
├── state/                serviços com RxJS/Signals para estado compartilhado
└── app.routes.ts         rotas protegidas por RoleGuard
```

**PWA**: manifest + service worker (`@angular/pwa`), Geolocation API para posição do membro em campo, funciona 100% offline para captação (dashboard/mapa em tempo real naturalmente exige conexão para quem está vendo).

## 5. Autenticação e perfis

**Fluxo**: login → Spring Security valida → JWT de acesso (curto) + refresh token → interceptor HTTP injeta `Authorization: Bearer` → `RoleGuard` no front → back-end revalida a permissão em cada endpoint, sempre escopado pelo `partido_id` do usuário autenticado.

**Matriz de permissões**

| Perfil | Financeiro | Equipe | Eleitores (PII completa) | Dashboard/Mapa | Aprovar despesa |
|---|---|---|---|---|---|
| Super Administrador da Plataforma | Não vê por padrão; total via modo suporte | Não vê por padrão; total via modo suporte | Só via modo suporte (break-glass, com motivo e log) | Métricas operacionais de todos os partidos (sem conteúdo sensível) | Não |
| Administrador | Total (do seu partido) | Total | Total | Total | Sim |
| Candidato | Somente leitura (resumo) | Leitura | Agregado/anonimizado, sem PII bruta | Total, no nível geográfico do seu cargo | Não |
| Gerente Financeiro | Total | Não | Não | Só financeiro | Sim |
| Secretário (delegado) | Lança/edita, não aprova | Não | Não | Só financeiro | Só com delegação explícita |
| Líder de Equipe | Não | Gerencia sua equipe | Total (sua região) | Sua região | Não |
| Membro de Equipe | Não | Não | Cadastra/edita, sem excluir | Não | Não |

## 6. Estratégia de testes

### Back-end (JUnit 5, Mockito, Testcontainers, ArchUnit)

- **Domain**: JUnit 5 puro, sem contexto Spring — testa regra de negócio isolada (ex: validação de município obrigatório para PREFEITO/VEREADOR, cálculo de saldo do fundo eleitoral). Rápido, roda sem banco.
- **Application (casos de uso)**: JUnit 5 + Mockito, mockando as portas de saída (`RepositoryPort`, `PublisherPort`) para testar a orquestração sem tocar banco ou rede de verdade.
- **Infrastructure (adapters)**: testes de integração com **Testcontainers** — sobe PostgreSQL+PostGIS e Redis reais em container durante o teste, validando que os adapters JPA/PostGIS e Redis funcionam contra infraestrutura real, não só contra mock.
- **Contract/API**: `@SpringBootTest` + `MockMvc`/`WebTestClient` testando os controllers, incluindo se cada perfil é bloqueado/liberado corretamente nos endpoints (valida a matriz de permissões na prática, não só na intenção).
- **Arquitetura**: ArchUnit no pipeline, quebrando o build se `domain` importar Spring/JPA — garante que a separação hexagonal não se corrompe com o tempo.
- **Isolamento multi-tenant** (categoria própria, obrigatória dado o risco legal envolvido): teste de integração populando dois partidos no mesmo banco e garantindo que uma consulta feita como Partido A nunca retorna dado do Partido B, mesmo simulando bug de aplicação — valida a Row-Level Security de fato, não só na teoria.
- **Cobertura**: JaCoCo com meta mínima (ex: 70-80% em `domain`/`application`, onde está a regra de negócio), como gate no CI/CD.

### Front-end (Angular 19)

- **Unit**: recomendo **Jest** em vez do Karma/Jasmine padrão do Angular — mais rápido e mais simples de rodar headless no CI (evita a complexidade de configurar Chrome headless).
- **Componentes**: Angular Testing Library/TestBed, testando isoladamente peças críticas (canvas de assinatura, formulário de eleitor, componente de mapa).
- **Serviços críticos**: testes dedicados para `RealtimeService` (mock de WebSocket) e `OfflineStore`/`SyncService` (mock de IndexedDB) — simulando perda de conexão, fila de sincronização, reconexão e conflito de upsert. É a parte mais nova e arriscada do projeto, merece prioridade de teste.
- **E2E**: recomendo **Playwright** em vez de Cypress especificamente por ele emular modo offline nativamente (`context.setOffline(true)`) e simular geolocalização/viewport mobile — encaixa direto no fluxo de campo do sistema. Cobre fluxos críticos: login por perfil, cadastro de eleitor, abordagem com intenção de voto para 1 ou vários candidatos, consentimento (assinatura e QR code), e o ciclo completo offline → sincronização → dashboard atualizado.
- **Acessibilidade**: `axe-core` integrado aos testes de componente, para pegar problemas básicos de contraste/leitura relevantes ao uso em campo sob luz solar por usuários pouco técnicos.

## Pendências fora do código

- Revisão do termo de consentimento (TermoConsentimento) e da política de retenção/expurgo de dados por advogado especialista em LGPD.
- Confirmação de que o módulo financeiro complementa — e não substitui — a prestação de contas oficial no sistema do TSE (SPCE).
- Confirmar viabilidade jurídica de qualquer monetização cruzada entre partidos antes de implementar (recomendação atual: não implementar).
