package com.campanha.partido.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidoDomainTest {

    @Test
    void rejeitaSemNome() {
        assertThrows(IllegalArgumentException.class,
                () -> new Partido(null, "", "PX", 30, "12345678000199", null, null, null, null, "FREE", true, Instant.now()));
    }

    @Test
    void rejeitaSemSigla() {
        assertThrows(IllegalArgumentException.class,
                () -> new Partido(null, "Partido X", "", 30, "12345678000199", null, null, null, null, "FREE", true, Instant.now()));
    }

    @Test
    void rejeitaNumeroPartidoForaDeFaixa() {
        assertThrows(IllegalArgumentException.class,
                () -> new Partido(null, "Partido X", "PX", 5, "12345678000199", null, null, null, null, "FREE", true, Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Partido(null, "Partido X", "PX", 100, "12345678000199", null, null, null, null, "FREE", true, Instant.now()));
    }
}
