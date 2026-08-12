package com.campanha.consentimento.infrastructure;

import com.campanha.consentimento.infrastructure.adapter.out.whatsapp.WhatsAppOptInAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppStubTest {

    @Test
    void stubRetornaIdComPrefixoStubENuncaFalha() {
        WhatsAppOptInAdapter stub = new WhatsAppOptInAdapter();
        String id = stub.confirmarOptIn("5511999999999", "Fulano", "COD001");
        assertNotNull(id);
        assertTrue(id.startsWith("STUB-"),
                "stub deveria retornar id iniciando com STUB- (para não ser confundido com id real da Meta)");
    }

    @Test
    void stubNaoFalhaComTelefoneNulo() {
        WhatsAppOptInAdapter stub = new WhatsAppOptInAdapter();
        // Não bloqueamos telefones malformados no adapter (validação fica no service).
        assertDoesNotThrow(() -> stub.confirmarOptIn(null, "Fulano", "X"));
    }
}
