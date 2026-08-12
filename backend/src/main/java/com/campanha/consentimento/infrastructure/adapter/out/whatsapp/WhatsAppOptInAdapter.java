package com.campanha.consentimento.infrastructure.adapter.out.whatsapp;

import com.campanha.consentimento.application.port.out.WhatsAppOptInPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STUB (D-03) do adapter de WhatsApp. Loga em nível estruturado e retorna
 * um id fake, sem chamar API externa. A integração real (Meta Cloud API ou
 * Twilio) vira skill separada — depende de:
 *
 * - Cadastro de negócio verificado na Meta
 * - Número de telefone dedicado
 * - Template de mensagem aprovado
 *
 * TODO(D-03): substituir por integração real.
 */
@Component
@Slf4j
public class WhatsAppOptInAdapter implements WhatsAppOptInPort {

    @Override
    public String confirmarOptIn(String telefoneE164, String nomeCandidato, String cod) {
        String stubId = "STUB-" + UUID.randomUUID();
        log.info("WHATSAPP_STUB action=confirm_optin telefone={} candidato={} cod={} stub_id={}",
                telefoneE164, nomeCandidato, cod, stubId);
        return stubId;
    }
}
