package com.campanha.auditoria.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SolicitacaoAcessoSuporteDomainTest {

    private SolicitacaoAcessoSuporte pendente() {
        return new SolicitacaoAcessoSuporte(
                1L, 100L, 42L, "investigar bug", "eleitores",
                Instant.now(), StatusSolicitacaoSuporte.PENDENTE,
                null, null, false, null, null);
    }

    @Test
    void aprovarPendenteMudaStatusEPreencheAprovador() {
        SolicitacaoAcessoSuporte s = pendente().aprovar(200L, false);
        assertEquals(StatusSolicitacaoSuporte.APROVADA, s.status());
        assertEquals(200L, s.aprovadorId());
        assertNotNull(s.aprovadaEm());
        assertFalse(s.aprovacaoFallback());
    }

    @Test
    void aprovarComFallbackMarcaFlag() {
        SolicitacaoAcessoSuporte s = pendente().aprovar(200L, true);
        assertTrue(s.aprovacaoFallback());
    }

    @Test
    void aprovarNaoPendenteLancaEstadoInvalido() {
        SolicitacaoAcessoSuporte aprovada = pendente().aprovar(200L, false);
        assertThrows(IllegalStateException.class, () -> aprovada.aprovar(300L, false));
    }

    @Test
    void solicitanteNaoPodeSerOProprioAprovador() {
        assertThrows(IllegalArgumentException.class,
                () -> pendente().aprovar(100L, false));
    }

    @Test
    void negarExigeMotivoENaoAprovadaDepois() {
        SolicitacaoAcessoSuporte s = pendente().negar(200L, "escopo insuficiente");
        assertEquals(StatusSolicitacaoSuporte.NEGADA, s.status());
        assertEquals("escopo insuficiente", s.motivoNegacao());
        assertThrows(IllegalStateException.class, () -> s.aprovar(300L, false));
    }

    @Test
    void motivoObrigatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitacaoAcessoSuporte(null, 100L, 42L, "", "escopo",
                        Instant.now(), null, null, null, false, null, null));
    }
}
