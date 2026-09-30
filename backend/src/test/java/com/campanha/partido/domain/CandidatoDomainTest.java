package com.campanha.partido.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CandidatoDomainTest {

    @ParameterizedTest
    @EnumSource(value = Cargo.class, names = {"PREFEITO", "VEREADOR"})
    void deveExigirMunicipioParaCargosMunicipais(Cargo cargo) {
        MunicipioObrigatorioException ex = assertThrows(
                MunicipioObrigatorioException.class,
                () -> new Candidato(null, 1L, null, "Fulano", "12345", 55, cargo, "SP", null, true, Instant.now())
        );
        assertTrue(ex.getMessage().contains(cargo.name()));
    }

    @ParameterizedTest
    @EnumSource(value = Cargo.class, names = {"PRESIDENTE", "GOVERNADOR", "SENADOR", "DEPUTADO_FEDERAL", "DEPUTADO_ESTADUAL"})
    void naoExigeMunicipioParaCargosNacionaisEEstaduais(Cargo cargo) {
        Candidato c = new Candidato(null, 1L, null, "Fulano", "12345", 55, cargo, "SP", null, true, Instant.now());
        assertNull(c.municipio());
        assertEquals(cargo, c.cargo());
    }

    @Test
    void aceitaCargoMunicipalComMunicipio() {
        Candidato c = new Candidato(null, 1L, null, "Fulano", "12345", 55, Cargo.PREFEITO, "SP", "São Paulo", true, Instant.now());
        assertEquals("São Paulo", c.municipio());
    }

    @Test
    void rejeitaUfInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Candidato(null, 1L, null, "Fulano", "12345", 55, Cargo.SENADOR, "XXX", null, true, Instant.now()));
    }

    @Test
    void rejeitaSemPartido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Candidato(null, null, null, "Fulano", "12345", 55, Cargo.SENADOR, "SP", null, true, Instant.now()));
    }
}
