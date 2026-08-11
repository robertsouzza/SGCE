package com.campanha.eleitores.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EleitorAnonimizacaoTest {

    private Eleitor eleitorReal() {
        return new Eleitor(
                42L, 1L, "João da Silva", "Rua X, 123",
                new Ponto(-46.6, -23.5), "11999999999",
                "123456789012", null, "001", "0042", "observação",
                false, null, Instant.now(), Instant.now());
    }

    @Test
    void anonimizarApagaPiiMasPreservaIdEPartido() {
        Eleitor e = eleitorReal().anonimizar();
        assertTrue(e.anonimizado());
        assertNotNull(e.anonimizadoEm());
        assertEquals(42L, e.id());
        assertEquals(1L, e.partidoId());
        assertNull(e.endereco());
        assertNull(e.geolocalizacao());
        assertNull(e.telefoneWhatsapp());
        assertNull(e.tituloEleitor());
        assertNull(e.zonaEleitoral());
        assertNull(e.secaoEleitoral());
        assertNull(e.observacoes());
        assertEquals("Eleitor anonimizado #42", e.nomeCompleto());
    }

    @Test
    void anonimizarPreservaHashDoTituloParaDedupe() {
        Eleitor e = eleitorReal().anonimizar();
        assertNotNull(e.tituloEleitorHash());
        assertEquals(64, e.tituloEleitorHash().length()); // SHA-256 hex
        // Determinístico:
        Eleitor e2 = eleitorReal().anonimizar();
        assertEquals(e.tituloEleitorHash(), e2.tituloEleitorHash());
    }

    @Test
    void construtorRejeitaEleitorNaoAnonimizadoSemNomeOuTitulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Eleitor(null, 1L, "", "end", null, null, "12345", null,
                        null, null, null, false, null, Instant.now(), Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Eleitor(null, 1L, "João", "end", null, null, "", null,
                        null, null, null, false, null, Instant.now(), Instant.now()));
    }

    @Test
    void construtorAceitaEleitorAnonimizadoSemPii() {
        Eleitor anon = new Eleitor(1L, 1L, "Eleitor anonimizado #1", null, null,
                null, null, "hash", null, null, null, true, Instant.now(),
                Instant.now(), Instant.now());
        assertTrue(anon.anonimizado());
    }
}
