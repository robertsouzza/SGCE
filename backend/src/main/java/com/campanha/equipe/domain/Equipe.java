package com.campanha.equipe.domain;

import java.time.Instant;

public record Equipe(
        Long id,
        Long partidoId,
        String nome,
        Long liderId,
        String regiaoAtuacao,
        Instant criadoEm
) {
    public Equipe {
        if (partidoId == null) {
            throw new IllegalArgumentException("equipe precisa estar vinculada a um partido");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome da equipe é obrigatório");
        }
        if (liderId == null) {
            throw new IllegalArgumentException("equipe precisa ter um líder");
        }
    }
}
