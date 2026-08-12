package com.campanha.consentimento.domain;

import com.campanha.eleitores.domain.Ponto;

import java.time.Instant;

public record ConsentimentoLGPD(
        Long id,
        Long partidoId,
        Long eleitorId,
        Long abordagemId,
        Long termoVersaoId,
        MetodoCaptura metodoCaptura,
        String assinaturaArquivoUrl,
        Long membroCapturaId,
        Ponto geolocalizacao,
        Instant timestampLocal,
        Instant timestampSincronizacao,
        boolean contatoSalvoConfirmado,
        String cod,
        EstadoConsentimento consentimentoDados,
        EstadoConsentimento consentimentoWhatsappMarketing,
        Instant criadoEm
) {
    public ConsentimentoLGPD {
        if (partidoId == null || eleitorId == null || termoVersaoId == null || membroCapturaId == null) {
            throw new IllegalArgumentException("partido, eleitor, termo e membro são obrigatórios");
        }
        if (metodoCaptura == null) {
            throw new IllegalArgumentException("metodoCaptura é obrigatório");
        }
        if (metodoCaptura == MetodoCaptura.ASSINATURA_TELA && assinaturaArquivoUrl == null) {
            throw new IllegalArgumentException(
                    "assinatura em tela requer assinaturaArquivoUrl");
        }
        if (consentimentoDados == null || consentimentoWhatsappMarketing == null) {
            throw new IllegalArgumentException(
                    "estados de consentimento dados/whatsapp são obrigatórios");
        }
    }

    /**
     * Revoga apenas o consentimento_dados. Dispara anonimização do Eleitor
     * no orquestrador (RevogarConsentimentoDadosUseCase — D-02). Aqui só
     * a máquina de estado.
     */
    public ConsentimentoLGPD revogarDados() {
        return new ConsentimentoLGPD(
                id, partidoId, eleitorId, abordagemId, termoVersaoId,
                metodoCaptura, assinaturaArquivoUrl, membroCapturaId,
                geolocalizacao, timestampLocal, timestampSincronizacao,
                contatoSalvoConfirmado, cod,
                consentimentoDados.revogar(),
                consentimentoWhatsappMarketing,
                criadoEm);
    }

    /** Revoga só marketing WhatsApp — dados/agregados permanecem. */
    public ConsentimentoLGPD revogarWhatsapp() {
        return new ConsentimentoLGPD(
                id, partidoId, eleitorId, abordagemId, termoVersaoId,
                metodoCaptura, assinaturaArquivoUrl, membroCapturaId,
                geolocalizacao, timestampLocal, timestampSincronizacao,
                contatoSalvoConfirmado, cod,
                consentimentoDados,
                consentimentoWhatsappMarketing.revogar(),
                criadoEm);
    }

    public boolean tratamentoDadosVigente() {
        return consentimentoDados.estaVigente();
    }

    public boolean marketingWhatsappVigente() {
        return consentimentoWhatsappMarketing.estaVigente();
    }
}
