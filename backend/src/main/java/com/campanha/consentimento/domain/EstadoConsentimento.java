package com.campanha.consentimento.domain;

import java.time.Instant;

/**
 * Estado de uma flag de consentimento (concedido/revogado com timestamps).
 * Modelado como VO para o ConsentimentoLGPD ter duas instâncias independentes
 * (dados vs whatsapp_marketing) sem duplicar métodos de manipulação.
 */
public record EstadoConsentimento(
        boolean concedido,
        Instant concedidoEm,
        boolean revogado,
        Instant revogadoEm
) {
    public static EstadoConsentimento conceder() {
        return new EstadoConsentimento(true, Instant.now(), false, null);
    }

    public static EstadoConsentimento recusar() {
        return new EstadoConsentimento(false, null, false, null);
    }

    public EstadoConsentimento revogar() {
        if (!concedido) {
            throw new IllegalStateException(
                    "não é possível revogar consentimento nunca concedido");
        }
        if (revogado) {
            return this; // idempotente
        }
        return new EstadoConsentimento(concedido, concedidoEm, true, Instant.now());
    }

    public boolean estaVigente() {
        return concedido && !revogado;
    }
}
