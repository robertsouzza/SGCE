# Decisões complementares — SGCE

Este arquivo complementa `modelo-tecnico-sistema-campanha.md` e `requisitos-funcionais-nao-funcionais.md` com decisões tomadas depois da primeira redação daqueles documentos, para fechar ambiguidades que travariam a geração de código.

**Leia este arquivo antes de gerar código.** Se algum ponto abaixo conflitar com os outros dois documentos, este vence — foi decidido depois.

---

## Consentimento e LGPD

### D-01 · Fluxo do QR code de consentimento

O QR code apresentado ao eleitor abre um **deep-link `wa.me`** com mensagem de opt-in pré-preenchida, direcionada ao número da campanha do partido. Exemplo:

```
https://wa.me/55DDDNUMERO?text=Autorizo%20receber%20conte%C3%BAdo%20do%20candidato%20FULANO%20(cod%3AABC123)
```

- O eleitor só precisa apertar "enviar" no próprio WhatsApp — cobre juridicamente o `consentimento_whatsapp_marketing` exigido pela Res. TSE 23.610 e resolve o problema de "salvar o contato" (a lista de transmissão do WhatsApp só entrega se o eleitor tiver salvo o número, e a mensagem de opt-in inicia a conversa a partir do lado do eleitor).
- **Não** cria endpoint público nem módulo `consentimento-publico`. Toda a captura ainda acontece pelo app do membro; o QR é gerado no cliente com base nos dados da abordagem em curso.
- O `cod` na mensagem é um ID curto que amarra a resposta recebida no WhatsApp de volta à `Abordagem`/`ConsentimentoLGPD` correspondente quando a integração real de WhatsApp for feita (ver D-03).

### D-02 · Retenção após revogação de `consentimento_dados`

Quando o eleitor revoga o `consentimento_dados`, o sistema **anonimiza**, não deleta:

- `Eleitor`: PII apagada — `nome_completo` vira `"Eleitor anonimizado #<id>"`, `endereco`, `telefone_whatsapp`, `geolocalizacao`, `titulo_eleitor`, `zona_eleitoral`, `secao_eleitoral`, `observacoes` viram `NULL`.
- `ConsentimentoLGPD`: mantido intacto — é prova histórica de que o consentimento existiu e foi revogado, com data. Não deletar.
- `Abordagem` e `IntencaoVoto`: mantidas. Sem PII vinculável (o `Eleitor` está anonimizado), continuam servindo às métricas agregadas por região/candidato.
- A revogação do `consentimento_whatsapp_marketing` (sozinha, sem revogação de dados) apenas seta a flag e para o envio; não anonimiza nada.

Implementar como um caso de uso `AnonimizarEleitorUseCase` disparado pelo endpoint de revogação.

### D-10 · Consentimento de geolocalização dos voluntários

Membros de equipe têm seu próprio consentimento LGPD, separado do consentimento do eleitor:

- **Termo específico no onboarding do membro**, assinado ao cadastrar-se como voluntário. Texto próprio explicando: rastreamento em tempo real, finalidade (coordenação em campo), dados coletados (posição, timestamp, status de conexão), quem vê (líder da equipe, administrador do partido).
- **Toggle "modo campo" no app**, visível e reversível a qualquer momento. Fora do modo campo, **nenhum heartbeat é enviado** — o membro fica offline no dashboard de tempo real, mesmo com o app aberto.
- O termo é uma nova entidade (`TermoConsentimentoMembro`, análoga a `TermoConsentimento` do eleitor) e o consentimento é `ConsentimentoMembro`, também versionado.

---

## Multi-tenancy e segurança

### D-05 · RLS + pool de conexão HikariCP

Row-Level Security no Postgres funciona por sessão. Com pool, se `SET app.current_partido_id = X` não for revertido, a próxima requisição que pegar a mesma conexão herda o tenant errado — vazamento silencioso.

**Padrão a adotar:** `SET LOCAL app.current_partido_id = :tenant` **dentro de transação**. `SET LOCAL` só vale até `COMMIT`/`ROLLBACK`, então quando a conexão volta ao pool, o valor já morreu — sem necessidade de listener do Hikari.

Implicação: **todo request que toca dados de tenant precisa estar em uma transação** (Spring `@Transactional` no serviço ou no filtro que aplica o `SET LOCAL`). Adicionar teste explícito no ArchUnit ou em `WebMvcTest` que garante isso.

### D-08 · JWT storage no frontend

Token JWT vive em **cookie httpOnly + Secure + SameSite=Lax**, nunca em `localStorage`/`sessionStorage`.

- Backend seta via `Set-Cookie` no login/refresh. Frontend nunca lê o valor.
- CSRF token separado (double-submit cookie ou `X-XSRF-TOKEN` header) para endpoints de mutação.
- CORS configurado com `Access-Control-Allow-Credentials: true` e origem explícita (não `*`).
- **Arquitetura de deploy:** nginx reverso no `docker-compose` servindo frontend em `/` e proxy do backend em `/api` — mesma origem, cookie funciona sem drama de cross-site.

Interceptor Angular remove o padrão de anexar `Authorization: Bearer` — o cookie viaja sozinho. Endpoint `/api/csrf-token` (GET) retorna o token CSRF; interceptor injeta em toda requisição `POST`/`PUT`/`PATCH`/`DELETE` como header.

### D-09 · Break-glass do Super Administrador — dual-control

Iniciar sessão de "modo suporte" para acessar dados de um partido específico exige aprovação de um segundo aprovador:

1. Super Admin A abre solicitação (motivo, escopo, tempo estimado) via endpoint.
2. Segundo aprovador recebe notificação e aprova/nega na UI dedicada.
3. Só após aprovação, a sessão de suporte é criada em `AcessoSuporteLog` e o Super Admin A ganha acesso; expira automaticamente pelo `expira_em`.

**Quem é o segundo aprovador:** outro `SUPER_ADMIN_PLATAFORMA`. **Fallback:** se só houver 1 Super Admin ativo, o `ADMIN` do partido cujos dados serão acessados vira o aprovador (garante que ninguém acessa dados de um partido sem o partido saber). O fallback fica registrado na tabela de aprovação.

Nova entidade: `SolicitacaoAcessoSuporte` (id, solicitante_id, partido_alvo_id, motivo, escopo, criada_em, status [PENDENTE/APROVADA/NEGADA/EXPIRADA], aprovador_id, aprovada_em, aprovacao_fallback bool).

---

## Sincronização e offline

### D-04 · Resolução de conflito na sincronização offline

Quando dois membros editam offline o mesmo eleitor (identificado por `titulo_eleitor`) e ambos sincronizam:

- **Last-write-wins pelo `timestamp_local` do dispositivo** — o registro com timestamp mais recente vence.
- **Antes de sobrescrever, o registro anterior vai integralmente para `LogAuditoria` como `dados_antes`** — permite recovery manual se o líder de equipe perceber depois que um dado bom foi sobrescrito.
- O membro que perdeu o overwrite recebe notificação assíncrona no próximo abrir do app: *"Sua edição do eleitor X foi substituída por uma edição mais recente feita por Y. Ver histórico."* — link abre visualização do `LogAuditoria` da entidade.
- Relógio do dispositivo pode estar errado (celular offline há dias). Se `timestamp_local` do incoming for **mais que 24h no futuro** em relação ao `timestamp_sincronizacao`, o servidor rejeita a linha e devolve erro para o app tratar (mostrar para o usuário: "verifique o relógio do seu dispositivo").

---

## Infraestrutura e integrações

### D-02b · Armazenamento de arquivos (comprovantes e assinaturas)

Adapter único usando **AWS SDK for Java (S3 client)**:

- **Dev/CI:** container **MinIO** no `docker-compose` (`minio/minio`), com bucket `sgce-uploads` criado por script de init.
- **Prod:** S3 real (ou qualquer S3-compatível — Backblaze, Wasabi, R2). Só muda endpoint e credencial em `application.yml`.
- Adapter Java: `com.campanha.shared.storage.S3StorageAdapter` implementa `ComprovanteStoragePort` e `AssinaturaStoragePort`. Uma implementação, dois ports.
- URLs geradas com **presigned URL** (expira em minutos) — o frontend baixa direto do MinIO/S3, backend não faz proxy do binário.
- Bucket policy nega listagem pública; leitura só via presigned.

### D-03 · Integração WhatsApp — stub no MVP

`WhatsAppOptInAdapter` no MVP é apenas um stub:

- Implementa a interface `WhatsAppOptInPort`.
- Grava a chamada em log estruturado (`logger.info("WHATSAPP_STUB action=confirm_optin eleitor_id={} cod={}", ...)`) e retorna sucesso.
- Comentário `TODO(D-03): substituir por integração real (Meta Cloud API ou Twilio) — ver decisao-tomadas.md`.
- Integração real vira uma skill separada (`sgce-whatsapp-integracao`), fora deste projeto de scaffolding.

O QR code (D-01) já funciona sem essa integração, porque o próprio celular do eleitor abre o WhatsApp — o backend não precisa mandar mensagem para o consentimento inicial. A integração real serve para: confirmação assíncrona depois da sincronização offline, mensagens de campanha (após opt-in), notificação de revogação processada.

### D-06 · Malha territorial IBGE (`RegiaoEleitoral`)

Fora do escopo da skill de geração:

- Migration Flyway cria só o **schema** de `RegiaoEleitoral` (com `geometria geometry(Polygon, 4326)`).
- Seed sintético em `db/migration/dev/`: 3 UFs fictícias (`SP-FAKE`, `RJ-FAKE`, `MG-FAKE`), ~10 municípios, ~30 bairros/zonas — polígonos de bounding box arbitrário, só para o mapa não ficar vazio em dev/teste.
- Script `scripts/import-ibge.sh` documentado no README, **não executado pela skill**: baixa shapefiles do IBGE (malha municipal + setores censitários), carrega via `shp2pgsql | psql`. Rodar manualmente antes de subir em prod.

---

## Padrões técnicos transversais

### D-11 · Fuso horário

- **Todos os timestamps no banco em `TIMESTAMP WITH TIME ZONE`**, sempre armazenados em UTC (Postgres normaliza).
- Java: `Instant` no domínio (nunca `LocalDateTime` sem TZ).
- `timestamp_local` (o horário do dispositivo, capturado offline) é um **campo separado** de `timestamp_sincronizacao`, também `TIMESTAMP WITH TIME ZONE` — o dispositivo envia com o offset local dele; o servidor não recalcula.
- Frontend exibe convertendo para o TZ do navegador via `Intl.DateTimeFormat`.

### D-12 · Versão do stack

Fixar no `pom.xml`/`package.json` no momento da geração:

- **Backend:** última versão estável de **Spring Boot 3.x compatível com Java 21** disponível na data da geração.
- **Frontend:** última versão estável de **Angular** disponível na data da geração (não fixar em "19" — quando a skill rodar, pode ser 20+).
- Versões pinadas explicitamente (não usar `LATEST` ou range), para o build ser reproduzível.
- Adicionar comentário na seção `## Stack técnica` do `SKILL.md` esclarecendo essa política antes de gerar dependências.
