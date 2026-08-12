package com.campanha.consentimento.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConsentimentoLGPDDomainTest {

    private ConsentimentoLGPD comAmbosConcedidos() {
        return new ConsentimentoLGPD(
                1L, 10L, 100L, 200L, 5L,
                MetodoCaptura.QRCODE_WHATSAPP, null, 42L,
                null, Instant.now(), Instant.now(),
                false, "COD001",
                EstadoConsentimento.conceder(),
                EstadoConsentimento.conceder(),
                Instant.now());
    }

    @Test
    void ambosVigentesAposCaptura() {
        ConsentimentoLGPD c = comAmbosConcedidos();
        assertTrue(c.tratamentoDadosVigente());
        assertTrue(c.marketingWhatsappVigente());
    }

    @Test
    void revogarDadosAfetaSoConsentimentoDeDados() {
        ConsentimentoLGPD c = comAmbosConcedidos().revogarDados();
        assertFalse(c.tratamentoDadosVigente());
        assertTrue(c.marketingWhatsappVigente(), "revogar dados NÃO deve afetar marketing WhatsApp");
        assertTrue(c.consentimentoDados().revogado());
        assertNotNull(c.consentimentoDados().revogadoEm());
    }

    @Test
    void revogarWhatsappAfetaSoMarketing() {
        ConsentimentoLGPD c = comAmbosConcedidos().revogarWhatsapp();
        assertTrue(c.tratamentoDadosVigente(), "revogar whatsapp NÃO deve afetar tratamento de dados");
        assertFalse(c.marketingWhatsappVigente());
        assertTrue(c.consentimentoWhatsappMarketing().revogado());
    }

    @Test
    void revogarDadosDuasVezesEIdempotente() {
        ConsentimentoLGPD primeira = comAmbosConcedidos().revogarDados();
        Instant primeiraVez = primeira.consentimentoDados().revogadoEm();
        ConsentimentoLGPD segunda = primeira.revogarDados();
        assertEquals(primeiraVez, segunda.consentimentoDados().revogadoEm());
    }

    @Test
    void naoPodeRevogarConsentimentoNuncaConcedido() {
        EstadoConsentimento nunca = EstadoConsentimento.recusar();
        assertThrows(IllegalStateException.class, nunca::revogar);
    }

    @Test
    void assinaturaTelaExigeUrlNoConstrutor() {
        assertThrows(IllegalArgumentException.class, () -> new ConsentimentoLGPD(
                null, 10L, 100L, null, 5L,
                MetodoCaptura.ASSINATURA_TELA, null, 42L,
                null, null, Instant.now(),
                false, null,
                EstadoConsentimento.conceder(), EstadoConsentimento.recusar(),
                Instant.now()));
    }
}
