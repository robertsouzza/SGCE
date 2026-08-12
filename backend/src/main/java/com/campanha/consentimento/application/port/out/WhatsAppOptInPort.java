package com.campanha.consentimento.application.port.out;

/**
 * Contrato para integração com WhatsApp. Implementação de MVP é um STUB
 * (D-03) que só loga. Integração real (Meta Cloud API ou Twilio) fica
 * fora deste roadmap — vira skill própria.
 */
public interface WhatsAppOptInPort {
    /** Envia mensagem de confirmação de opt-in ao eleitor. Retorna id da mensagem (ou stub id). */
    String confirmarOptIn(String telefoneE164, String nomeCandidato, String cod);
}
