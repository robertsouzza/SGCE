package com.campanha.consentimento.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.consentimento.application.port.in.ConsentimentoUseCases;
import com.campanha.consentimento.application.port.out.AssinaturaStoragePort;
import com.campanha.consentimento.application.port.out.ConsentimentoRepositoryPort;
import com.campanha.consentimento.application.port.out.TermoRepositoryPort;
import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.ConsentimentoMembro;
import com.campanha.consentimento.domain.EstadoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimentoMembro;
import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentimentoService implements ConsentimentoUseCases {

    private final TermoRepositoryPort termoRepo;
    private final ConsentimentoRepositoryPort consentimentoRepo;
    private final AssinaturaStoragePort assinaturaStorage;
    private final EleitorRepositoryPort eleitorRepo;
    private final DeepLinkService deepLinkService;

    @Override
    @Transactional
    @Auditavel(acao = "publicar_termo_eleitor", entidade = "TermoConsentimento")
    public TermoConsentimento publicarTermoEleitor(String texto) {
        Long partido = tenantObrigatorio();
        int versao = termoRepo.proximaVersaoParaPartido(partido);
        return termoRepo.save(new TermoConsentimento(
                null, partido, versao, texto, Instant.now(), null, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "publicar_termo_membro", entidade = "TermoConsentimentoMembro")
    public TermoConsentimentoMembro publicarTermoMembro(String texto) {
        Long partido = tenantObrigatorio();
        int versao = termoRepo.proximaVersaoMembroParaPartido(partido);
        return termoRepo.saveMembro(new TermoConsentimentoMembro(
                null, partido, versao, texto, Instant.now(), null, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "capturar_consentimento", entidade = "ConsentimentoLGPD")
    public ConsentimentoLGPD capturarConsentimento(CapturarConsentimentoCommand cmd) {
        Long partido = tenantObrigatorio();
        TermoConsentimento termo = termoRepo.findVigenteMaisRecentePorPartido(partido)
                .orElseThrow(() -> new IllegalStateException(
                        "nenhum TermoConsentimento vigente para o partido — publique um antes de capturar"));
        Long membroId = usuarioIdAutenticado();

        EstadoConsentimento dados = cmd.consentimentoDados()
                ? EstadoConsentimento.conceder() : EstadoConsentimento.recusar();
        EstadoConsentimento wpp = cmd.consentimentoWhatsappMarketing()
                ? EstadoConsentimento.conceder() : EstadoConsentimento.recusar();

        String cod = codCurto();

        return consentimentoRepo.save(new ConsentimentoLGPD(
                null, partido, cmd.eleitorId(), cmd.abordagemId(), termo.id(),
                cmd.metodoCaptura(),
                cmd.metodoCaptura() == com.campanha.consentimento.domain.MetodoCaptura.QRCODE_WHATSAPP
                        ? "qrcode://gerado" : null, // provisório — anexarAssinatura preenche para ASSINATURA_TELA
                membroId, cmd.geolocalizacao(),
                cmd.timestampLocal(), Instant.now(),
                false, cod,
                dados, wpp, Instant.now()));
    }

    @Override
    @Transactional
    public ConsentimentoLGPD anexarAssinatura(Long consentimentoId, InputStream content,
                                              long contentLength, String nomeArquivo) {
        ConsentimentoLGPD c = consentimentoRepo.findById(consentimentoId)
                .orElseThrow(() -> new IllegalArgumentException("consentimento não encontrado"));
        String key = "assinaturas/" + c.partidoId() + "/" + c.eleitorId() + "/"
                + UUID.randomUUID() + "-" + sanitize(nomeArquivo);
        try {
            assinaturaStorage.save(key, content, contentLength, "image/png");
        } catch (IOException e) {
            throw new IllegalStateException("falha ao salvar assinatura: " + e.getMessage(), e);
        }
        return consentimentoRepo.save(new ConsentimentoLGPD(
                c.id(), c.partidoId(), c.eleitorId(), c.abordagemId(), c.termoVersaoId(),
                c.metodoCaptura(), key, c.membroCapturaId(),
                c.geolocalizacao(), c.timestampLocal(), c.timestampSincronizacao(),
                c.contatoSalvoConfirmado(), c.cod(),
                c.consentimentoDados(), c.consentimentoWhatsappMarketing(), c.criadoEm()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "revogar_consentimento_dados", entidade = "ConsentimentoLGPD")
    public ConsentimentoLGPD revogarConsentimentoDados(Long consentimentoId) {
        ConsentimentoLGPD c = consentimentoRepo.findById(consentimentoId)
                .orElseThrow(() -> new IllegalArgumentException("consentimento não encontrado"));

        // 1) Atualiza o ConsentimentoLGPD com a revogação (mantém a linha como
        //    prova histórica — D-02).
        ConsentimentoLGPD revogado = consentimentoRepo.save(c.revogarDados());

        // 2) Anonimiza o Eleitor: apaga PII, mantém id, hash do título e
        //    preserva Abordagem/IntencaoVoto para agregados por região.
        Eleitor eleitor = eleitorRepo.findById(c.eleitorId())
                .orElseThrow(() -> new IllegalStateException(
                        "eleitor do consentimento não encontrado (id=" + c.eleitorId() + ")"));
        Eleitor anonimizado = eleitor.anonimizar();
        eleitorRepo.save(anonimizado);

        log.info("consentimento_dados revogado; Eleitor {} anonimizado", eleitor.id());
        return revogado;
    }

    @Override
    @Transactional
    @Auditavel(acao = "revogar_consentimento_whatsapp", entidade = "ConsentimentoLGPD")
    public ConsentimentoLGPD revogarConsentimentoWhatsApp(Long consentimentoId) {
        ConsentimentoLGPD c = consentimentoRepo.findById(consentimentoId)
                .orElseThrow(() -> new IllegalArgumentException("consentimento não encontrado"));
        return consentimentoRepo.save(c.revogarWhatsapp());
    }

    @Override
    @Transactional
    public DeepLinkOptInResult gerarDeepLinkOptIn(Long abordagemId, Long candidatoId) {
        return deepLinkService.gerar(abordagemId, candidatoId, tenantObrigatorio());
    }

    @Override
    @Transactional
    @Auditavel(acao = "capturar_consentimento_membro", entidade = "ConsentimentoMembro")
    public ConsentimentoMembro capturarConsentimentoMembro(Long usuarioId, boolean concedido) {
        Long partido = tenantObrigatorio();
        TermoConsentimentoMembro termo = termoRepo.findVigenteMembroMaisRecentePorPartido(partido)
                .orElseThrow(() -> new IllegalStateException(
                        "nenhum TermoConsentimentoMembro vigente para o partido"));
        EstadoConsentimento estado = concedido
                ? EstadoConsentimento.conceder() : EstadoConsentimento.recusar();
        return consentimentoRepo.saveMembro(new ConsentimentoMembro(
                null, partido, usuarioId, termo.id(), estado, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "revogar_consentimento_membro", entidade = "ConsentimentoMembro")
    public ConsentimentoMembro revogarConsentimentoMembro(Long usuarioId) {
        ConsentimentoMembro cm = consentimentoRepo.findMembroPorUsuario(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "consentimento do membro não encontrado"));
        return consentimentoRepo.saveMembro(cm.revogarRastreamento());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean consentimentoRastreamentoAtivo(Long usuarioId) {
        return consentimentoRepo.findMembroPorUsuario(usuarioId)
                .map(ConsentimentoMembro::rastreamentoVigente)
                .orElse(false);
    }

    private Long tenantObrigatorio() {
        Long t = TenantContext.get();
        if (t == null) {
            throw new AccessDeniedException("operação requer contexto de partido");
        }
        return t;
    }

    private Long usuarioIdAutenticado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser au) {
            return au.usuarioId();
        }
        throw new AccessDeniedException("autenticação ausente");
    }

    private String codCurto() {
        // Base 36 do first 6 chars de UUID sem hífen — ~36^6 = 2B combinações.
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String sanitize(String name) {
        if (name == null) return "assinatura.png";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
