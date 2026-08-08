---
name: sgce-05-modulo-consentimento
description: Gera o módulo consentimento do backend SGCE — TermoConsentimento versionado (eleitor) e TermoConsentimentoMembro (voluntário), ConsentimentoLGPD com duas flags independentes (dados e whatsapp_marketing), fluxo de anonimização ao revogar consentimento_dados (apaga PII do Eleitor mas preserva ConsentimentoLGPD e agregados), geração de deep-link wa.me para QR code de opt-in de WhatsApp, e WhatsAppOptInAdapter como stub. Sexta skill do roadmap, roda após sgce-04-modulo-eleitores. Use quando o usuário quiser implementar captura/revogação de consentimento LGPD, gerar QR code de opt-in ou tratar o consentimento de rastreamento dos voluntários.
---

# sgce-05-modulo-consentimento — ConsentimentoLGPD (eleitor + voluntário), Anonimização, QR wa.me

## Contexto

Sexta skill. Módulo denso em regra jurídica. Cobre RF-15, RF-16 (eleitor) e implementa D-10 (voluntário) e D-02 (anonimização). Também gera o deep-link `wa.me` (D-01) e coloca o stub de WhatsApp (D-03).

**Assume:** skills 00–04 (backend + partido/candidato + equipe/membro + financeiro + eleitores prontos).

## Referências obrigatórias

- `../sgce-fullstack/SKILL.md`
- `../sgce-fullstack/references/modelo-tecnico-sistema-campanha.md` — Módulo Consentimento LGPD (seção 1)
- `../sgce-fullstack/references/requisitos-funcionais-nao-funcionais.md` — RF-15, RF-16, RNF-03, RNF-14
- `../sgce-fullstack/references/decisoes-tomadas.md` — D-01 (deep-link wa.me), D-02 (retenção pós-revogação = anonimização), D-03 (WhatsApp stub), D-10 (consentimento do voluntário)

## Passos

1. **Migrations** (`V20__termos_consentimento.sql`, `V21__consentimentos_lgpd.sql`, `V22__termos_e_consentimentos_membro.sql`):
   - `termos_consentimento`: `partido_id`, `versao INT`, `texto TEXT`, `vigente_a_partir TIMESTAMPTZ`, `vigente_ate TIMESTAMPTZ NULL`. Único `(partido_id, versao)`.
   - `consentimentos_lgpd`: FK `eleitor_id`, `abordagem_id`, `termo_versao_id`, `metodo_captura` (ASSINATURA_TELA, QRCODE_WHATSAPP), `assinatura_arquivo_url` (chave S3, nullable), `membro_captura_id`, `geolocalizacao geometry(Point, 4326)`, `timestamp_local TIMESTAMPTZ`, `timestamp_sincronizacao TIMESTAMPTZ`, e **os 4 pares** de flags:
     - `consentimento_dados bool`, `consentimento_dados_em TIMESTAMPTZ`, `consentimento_dados_revogado bool`, `consentimento_dados_revogado_em TIMESTAMPTZ`
     - idem para `consentimento_whatsapp_marketing`
   - `contato_salvo_confirmado bool` (default false, atualizado quando skill 03 real de WhatsApp for feita).
   - `termos_consentimento_membro` + `consentimentos_membro`: análogos ao do eleitor, mas amarrados a `usuario_id` (o voluntário).
   - Todas com RLS. `consentimentos_lgpd` deriva `partido_id` do `Eleitor`.

2. **Módulo `consentimento`** (`com.campanha.consentimento`):
   - **Domain:** `TermoConsentimento`, `ConsentimentoLGPD` com métodos `revogarDados()`, `revogarWhatsApp()`, `estaVigente(agora)`. `TermoConsentimentoMembro`, `ConsentimentoMembro`.
   - **Application:**
     - Ports in: `PublicarTermoUseCase`, `CapturarConsentimentoUseCase`, `RevogarConsentimentoDadosUseCase`, `RevogarConsentimentoWhatsAppUseCase`, `GerarDeepLinkOptInUseCase`, `PublicarTermoMembroUseCase`, `CapturarConsentimentoMembroUseCase`.
     - Ports out: `TermoRepositoryPort`, `ConsentimentoRepositoryPort`, `AssinaturaStoragePort` (implementada por `S3StorageAdapter`), `WhatsAppOptInPort`.
   - **Infrastructure:**
     - `ConsentimentoController`, `TermoController`, `ConsentimentoMembroController`, `DeepLinkController`.
     - `WhatsAppOptInAdapter` (STUB — D-03): implementa a port, só `logger.info("WHATSAPP_STUB action=... eleitor_id=... cod=...")` e retorna sucesso. Comentário `TODO(D-03): substituir por integração real (Meta Cloud API ou Twilio) — ver decisoes-tomadas.md`.

3. **Anonimização (D-02)**:
   - `RevogarConsentimentoDadosUseCase.executar(eleitorId, motivo)`:
     1. Localiza `Eleitor`.
     2. Chama `eleitor.anonimizar()` (método do domínio da skill 04) — `nome_completo` vira `"Eleitor anonimizado #{id}"`, `endereco/telefone_whatsapp/geolocalizacao/titulo_eleitor/zona_eleitoral/secao_eleitoral/observacoes` → NULL.
     3. `EleitorRepositoryPort.atualizar(...)`.
     4. Atualiza `ConsentimentoLGPD` do eleitor: `consentimento_dados_revogado = true`, `consentimento_dados_revogado_em = now()`.
     5. **Não** deleta Abordagem/IntencaoVoto — ficam sem PII vinculável (Eleitor anonimizado), preservando agregados por região.
     6. Grava tudo no `LogAuditoria`.
   - `RevogarConsentimentoWhatsAppUseCase`: só marca a flag, não anonimiza nada.

4. **Deep-link `wa.me` (D-01)** — `GerarDeepLinkOptInUseCase`:
   - Recebe `abordagem_id`, `candidato_id`.
   - Busca dados: número WhatsApp do candidato/partido, nome do candidato, gera `cod` curto único (Base62 do `consentimento_id` provisório).
   - Retorna URL:
     ```
     https://wa.me/{numero_e164_sem_mais}?text={mensagem_urlencoded}
     ```
     Mensagem-padrão: `"Autorizo receber conteúdo do candidato {nome} (cod:{cod})"`.
   - Salva o `cod` na `ConsentimentoLGPD` para amarrar a mensagem recebida futuramente (quando a integração real for feita).
   - Retorna também a URL como `data-URI` de PNG contendo o QR code (para o frontend renderizar sem depender de lib externa).

5. **Permissões**:
   - Capturar consentimento (eleitor): `MEMBRO_EQUIPE` ou acima, do partido dono da abordagem.
   - Revogar consentimento (eleitor): qualquer `MEMBRO_EQUIPE` do partido (o próprio eleitor pode acionar via endpoint público futuro — fora de escopo desta skill).
   - Publicar termo: só `ADMIN`.
   - Consentimento do voluntário: capturado no onboarding do próprio voluntário (endpoint só para o próprio usuário).

6. **Testes**:
   - Unit: `ConsentimentoLGPD.revogarDados()` seta as flags corretas; `estaVigente()` respeita revogações.
   - Application: `RevogarConsentimentoDadosUseCase` anonimiza Eleitor mas preserva Abordagem/IntencaoVoto e ConsentimentoLGPD.
   - Integration (Testcontainers): fluxo completo captura → agregados corretos → revogação → agregados ainda corretos (sem PII vinculável), assinatura no MinIO.
   - Testes específicos do deep-link: URL bem formada, `cod` único, QR code decodifica.
   - `WhatsAppOptInAdapterTest`: testa que o stub loga e retorna sucesso (para não regredir quando alguém for substituir).

## Definition of Done (verificável)

```bash
cd backend && ./mvnw test   # verde

# Manual:
# 1. Como ADMIN, POST /api/termos-consentimento → publica versão 1
# 2. Como MEMBRO_EQUIPE, cria eleitor + abordagem (skill 04)
# 3. POST /api/consentimentos com metodo_captura=ASSINATURA_TELA + arquivo PNG → 201, assinatura no MinIO
# 4. GET /api/deep-link-opt-in?abordagem_id=X&candidato_id=Y → retorna URL wa.me válida + QR PNG data-URI
# 5. GET /api/eleitores/{id} → tem PII completa
# 6. GET /api/eleitores/agregado?regiao_id=Z → conta esse eleitor
# 7. POST /api/consentimentos/{id}/revogar-dados → 200
# 8. GET /api/eleitores/{id} → nome = "Eleitor anonimizado #{id}", PII = null
# 9. GET /api/eleitores/agregado?regiao_id=Z → ainda conta (agregado preservado)
# 10. GET /api/consentimentos/{id} → consentimento_dados_revogado=true, timestamp preenchido
# 11. Log estruturado deve conter "WHATSAPP_STUB" nas linhas relevantes
```

## Notas para skills seguintes

- Skill 06 vai usar `TermoConsentimentoMembro` no onboarding do voluntário e o toggle "modo campo" no perfil do usuário.
- Skill 08 (frontend campo) usa o endpoint de deep-link para renderizar o QR na tela do membro, e implementa o canvas de assinatura.
- Skill de integração real de WhatsApp fica fora deste roadmap (fora do MVP).

## Referências adicionais (opcional para esta skill)

Se você criar um arquivo `references/template-termo-consentimento.md` nesta pasta com um texto-modelo de termo LGPD, o `PublicarTermoUseCase` pode carregar como default para novos partidos. **Não incluído por padrão** — texto real de termo LGPD deve ser revisado por advogado; ver `Pendências fora do código` no `modelo-tecnico-sistema-campanha.md`.
