package com.campanha.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DespesaDomainTest {

    private Despesa novaPendente() {
        return new Despesa(
                1L, 10L, 100L, CategoriaDespesa.MATERIAL_GRAFICO, null,
                new BigDecimal("500.00"), LocalDate.now(), "gráfica",
                42L, null, StatusDespesa.PENDENTE, null, null, null, Instant.now());
    }

    @Test
    void aprovarMudaStatusEPreencheAprovador() {
        Despesa d = novaPendente().aprovar(99L);
        assertEquals(StatusDespesa.APROVADO, d.status());
        assertEquals(99L, d.aprovadoPor());
        assertNotNull(d.aprovadoEm());
        assertNull(d.motivoRejeicao());
    }

    @Test
    void aprovarDuasVezesLancaEstadoInvalido() {
        Despesa aprovada = novaPendente().aprovar(99L);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> aprovada.aprovar(88L));
        assertTrue(ex.getMessage().contains("PENDENTE"));
    }

    @Test
    void rejeitarMudaStatusEExigeMotivo() {
        Despesa d = novaPendente().rejeitar(99L, "sem nota fiscal");
        assertEquals(StatusDespesa.REJEITADO, d.status());
        assertEquals("sem nota fiscal", d.motivoRejeicao());
    }

    @Test
    void rejeitarSemMotivoLancaValidacao() {
        assertThrows(IllegalArgumentException.class, () -> novaPendente().rejeitar(99L, ""));
        assertThrows(IllegalArgumentException.class, () -> novaPendente().rejeitar(99L, null));
    }

    @Test
    void rejeitarAprovadaLancaEstadoInvalido() {
        Despesa aprovada = novaPendente().aprovar(99L);
        assertThrows(IllegalStateException.class, () -> aprovada.rejeitar(88L, "outro motivo"));
    }

    @Test
    void valorZeroOuNegativoRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> new Despesa(null, 1L, 1L, CategoriaDespesa.OUTROS, null,
                        BigDecimal.ZERO, LocalDate.now(), null, 1L, null,
                        StatusDespesa.PENDENTE, null, null, null, Instant.now()));
    }

    @Test
    void comComprovantePreservaTudoMasAtualizaUrl() {
        Despesa d = novaPendente().comComprovante("s3://bucket/key");
        assertEquals("s3://bucket/key", d.comprovanteUrl());
        assertEquals(StatusDespesa.PENDENTE, d.status());
    }
}
