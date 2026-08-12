package com.campanha.consentimento.domain;

import java.time.Instant;

/**
 * Consentimento do voluntário para ser rastreado em tempo real durante o
 * modo campo (D-10). Verificado antes de cada heartbeat de localização
 * na skill 06.
 */
public record ConsentimentoMembro(
        Long id,
        Long partidoId,
        Long usuarioId,
        Long termoVersaoId,
        EstadoConsentimento consentimentoRastreamento,
        Instant criadoEm
) {
    public ConsentimentoMembro {
        if (partidoId == null || usuarioId == null || termoVersaoId == null) {
            throw new IllegalArgumentException("partido, usuario e termo são obrigatórios");
        }
        if (consentimentoRastreamento == null) {
            throw new IllegalArgumentException("consentimentoRastreamento é obrigatório");
        }
    }

    public ConsentimentoMembro revogarRastreamento() {
        return new ConsentimentoMembro(
                id, partidoId, usuarioId, termoVersaoId,
                consentimentoRastreamento.revogar(), criadoEm);
    }

    public boolean rastreamentoVigente() {
        return consentimentoRastreamento.estaVigente();
    }
}
