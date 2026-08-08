# Sistema de Gestão de Campanha Eleitoral 2026 — Requisitos

Complementa o modelo-tecnico-sistema-campanha.md. Serve de base para a geração da skill e, depois, como critério de aceite.

> **Leia também `decisoes-tomadas.md` antes de gerar código.** Alguns RFs abaixo (RF-15 QR consentimento, RF-16 revogação, RF-17 sync offline, RF-23 break-glass) têm o comportamento definitivo detalhado lá. Em caso de conflito, `decisoes-tomadas.md` vence.

## Requisitos Funcionais (RF)

### Partido / Multi-tenant

- **RF-01**: Cadastrar partidos (dados cadastrais, conta bancária partidária).
- **RF-02**: Isolar completamente os dados entre partidos distintos (nenhum partido acessa dado de outro).

### Candidatos

- **RF-03**: Cadastrar candidatos vinculados a um partido, com cargo (Presidente, Senador, Deputado Federal, Deputado Estadual, Prefeito, Vereador), UF e, quando aplicável, município.
- **RF-04**: Ajustar automaticamente o nível geográfico do dashboard/mapa de calor conforme o cargo do candidato (país, estado, município ou bairro/zona).

### Financeiro

- **RF-05**: Cadastrar recursos recebidos (fundo eleitoral, fundo partidário, doação) vinculados a um candidato.
- **RF-06**: Cadastrar despesas categorizadas (pessoal, alimentação, transporte, material gráfico, outros), com comprovante anexado.
- **RF-07**: Suportar pagamento de equipe por diária, salário, por abordagem ou por visita.
- **RF-08**: Fluxo de aprovação de despesas (lançamento por secretário delegado, aprovação por gerente financeiro).
- **RF-09**: Gerar relatório de uso de recursos em PDF, JSON e dashboard em tempo real.

### Equipe

- **RF-10**: Cadastrar líderes de equipe e membros vinculados.
- **RF-11**: Vincular uma equipe a um ou mais candidatos que está autorizada a promover, conforme orientação do partido.

### Eleitores e abordagem

- **RF-12**: Cadastrar eleitor com nome completo, endereço, geolocalização, telefone/WhatsApp, título e zona/seção eleitoral.
- **RF-13**: Registrar abordagem (domiciliar ou pública) com data, local e membro responsável.
- **RF-14**: Registrar intenção de voto por candidato dentro de uma mesma abordagem (1 ou vários candidatos da chapa).
- **RF-15**: Capturar consentimento LGPD do eleitor (assinatura em tela ou QR code no próprio celular), com consentimento separado para tratamento de dados e para recebimento de mensagens via WhatsApp.
- **RF-16**: Permitir revogação de cada consentimento (dados e/ou WhatsApp) a qualquer momento, de forma independente.
- **RF-17**: Funcionar totalmente offline na captação (cadastro de eleitor, abordagem, intenção, consentimento), sincronizando ao reconectar.

### Dashboard / Mapa em tempo real

- **RF-18**: Exibir em tempo real número de eleitores abordados, convencidos, e regiões com maior/menor intenção de voto.
- **RF-19**: Exibir localização em tempo real das equipes em campo (mapa ao vivo, estilo Uber).
- **RF-20**: Sinalizar visualmente regiões em coleta offline (equipe em campo sem conexão, dados pendentes de sincronização).

### Autenticação e perfis

- **RF-21**: Suportar os perfis Administrador, Candidato, Gerente Financeiro, Secretário, Líder de Equipe e Membro de Equipe, cada um com permissões próprias (ver matriz no modelo técnico).
- **RF-22**: Registrar toda ação sensível em log de auditoria (quem, quando, o quê, antes/depois).
- **RF-23**: Suportar o perfil Super Administrador da Plataforma, acima de todos os partidos, com visão padrão limitada a métricas operacionais agregadas (uso, erros, uptime) e acesso excepcional a dados de um partido específico apenas em "modo suporte" (motivo obrigatório, registrado em log, com expiração automática).

## Requisitos Não Funcionais (RNF)

### Desempenho

- **RNF-01**: Dashboard e mapa devem refletir novos eventos (abordagem, localização) em poucos segundos após a sincronização, via WebSocket.
- **RNF-02**: Suportar picos de uso concentrados perto da eleição (várias equipes lançando dados ao mesmo tempo) sem degradação perceptível.

### Segurança e LGPD

- **RNF-03**: Dados sensíveis (opinião política, arquivo de assinatura) criptografados em repouso e em trânsito (TLS).
- **RNF-04**: Toda permissão validada no back-end em cada requisição — nunca confiar apenas na trava do front-end.
- **RNF-05**: Política de retenção e expurgo de dados de eleitores definida e aplicável (ex: exclusão após período legal ou fim do vínculo).
- **RNF-06**: Isolamento multi-tenant garantido em nível de banco de dados (Row-Level Security no PostgreSQL), não só na camada de aplicação.

### Disponibilidade

- **RNF-07**: Sistema disponível durante todo o período de campanha (16/08 a outubro de 2026), com rotina de backup e plano de recuperação de desastre.
- **RNF-08**: A captação em campo (PWA offline) não deve depender da disponibilidade do backend para continuar funcionando.

### Escalabilidade

- **RNF-09**: Módulos da arquitetura hexagonal devem permitir extrair um módulo (ex: geolocalização) para serviço independente caso o volume justifique, sem reescrever a regra de negócio.

### Usabilidade e acessibilidade

- **RNF-10**: Interface de campo usável por voluntários com pouca familiaridade técnica, em celulares de entrada, com boa legibilidade sob luz solar direta.
- **RNF-11**: Funcionar em conexões de baixa qualidade (2G/3G intermitente), além do modo totalmente offline.

### Observabilidade

- **RNF-12**: Manter logs estruturados e métricas básicas (erros, latência, uso) para diagnóstico rápido durante o período crítico da campanha.

### Conformidade e auditoria

- **RNF-13**: Toda alteração em dados financeiros deve ser auditável e rastreável até o usuário responsável.
- **RNF-14**: O sistema não deve permitir exportação ou compartilhamento da base de eleitores entre partidos distintos, em nenhuma hipótese.
- **RNF-15**: Todo acesso do Super Administrador da Plataforma a dados de um partido específico (fora de métricas agregadas) deve ser um evento de "modo suporte" com motivo obrigatório, integralmente registrado e com expiração automática de sessão.

### Documentação de API

- **RNF-16**: Todos os endpoints devem ter documentação gerada automaticamente via OpenAPI/Swagger, disponível em ambiente de desenvolvimento para teste manual. Em produção, a documentação deve ficar desabilitada ou protegida por autenticação.

### Qualidade e testes

- **RNF-17**: Regras de negócio (camada `domain`) devem ter cobertura de teste unitário mínima (ex: 70-80%), medida por JaCoCo e validada no pipeline de CI/CD.
- **RNF-18**: Isolamento multi-tenant deve ter suíte de teste de integração dedicada (Testcontainers), validando que nenhuma consulta cruza dados entre partidos.
- **RNF-19**: O fluxo de sincronização offline → online deve ter cobertura de teste automatizado (unitário e E2E), cobrindo reconexão, fila de retry e conflito de upsert.
- **RNF-20**: O pipeline de CI/CD deve bloquear o merge se os testes de back-end (JUnit/Mockito/Testcontainers/ArchUnit) ou front-end (Jest/Playwright) falharem.
