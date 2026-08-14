package com.campanha.consentimento.application.port.in;

import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.ConsentimentoMembro;
import com.campanha.consentimento.domain.MetodoCaptura;
import com.campanha.consentimento.domain.TermoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimentoMembro;
import com.campanha.eleitores.domain.Ponto;

import java.io.InputStream;
import java.time.Instant;

public interface ConsentimentoUseCases {

    TermoConsentimento publicarTermoEleitor(String texto);

    TermoConsentimentoMembro publicarTermoMembro(String texto);

    ConsentimentoLGPD capturarConsentimento(CapturarConsentimentoCommand cmd);

    /** Anexa assinatura (PNG) a um consentimento ASSINATURA_TELA já criado. */
    ConsentimentoLGPD anexarAssinatura(Long consentimentoId, InputStream content, long contentLength, String nomeArquivo);

    /** Revoga só o consentimento_dados e dispara anonimização do Eleitor (D-02). */
    ConsentimentoLGPD revogarConsentimentoDados(Long consentimentoId);

    /** Revoga só marketing_whatsapp — dados/agregados permanecem. */
    ConsentimentoLGPD revogarConsentimentoWhatsApp(Long consentimentoId);

    DeepLinkOptInResult gerarDeepLinkOptIn(Long abordagemId, Long candidatoId);

    ConsentimentoMembro capturarConsentimentoMembro(Long usuarioId, boolean concedido);

    ConsentimentoMembro revogarConsentimentoMembro(Long usuarioId);

    /**
     * Verifica se o voluntário aceitou rastreamento e o consentimento
     * ainda está vigente (D-10). Consumido pela skill 06 antes de aceitar
     * um heartbeat de localização.
     */
    boolean consentimentoRastreamentoAtivo(Long usuarioId);

    record CapturarConsentimentoCommand(
            Long eleitorId,
            Long abordagemId,
            MetodoCaptura metodoCaptura,
            Ponto geolocalizacao,
            Instant timestampLocal,
            boolean consentimentoDados,
            boolean consentimentoWhatsappMarketing
    ) {}

    record DeepLinkOptInResult(
            String urlWaMe,
            String qrCodeDataUri,
            String cod
    ) {}
}
