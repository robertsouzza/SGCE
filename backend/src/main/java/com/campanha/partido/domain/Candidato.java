package com.campanha.partido.domain;

import java.time.Instant;

public record Candidato(
        Long id,
        Long partidoId,
        Long usuarioId,
        String nomeCompleto,
        String tituloEleitor,
        int numeroCandidato,
        Cargo cargo,
        String uf,
        String municipio,
        Instant criadoEm
) {
    public Candidato {
        if (partidoId == null) {
            throw new IllegalArgumentException("candidato precisa estar vinculado a um partido");
        }
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new IllegalArgumentException("nome completo do candidato é obrigatório");
        }
        if (tituloEleitor == null || tituloEleitor.isBlank()) {
            throw new IllegalArgumentException("título de eleitor do candidato é obrigatório");
        }
        if (cargo == null) {
            throw new IllegalArgumentException("cargo do candidato é obrigatório");
        }
        if (uf == null || uf.length() != 2) {
            throw new IllegalArgumentException("UF do candidato deve ter 2 letras");
        }
        if (cargo.exigeMunicipio() && (municipio == null || municipio.isBlank())) {
            throw new MunicipioObrigatorioException(cargo);
        }
    }
}
