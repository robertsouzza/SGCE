package com.campanha.equipe.domain;

import java.time.Instant;

public record MembroEquipe(
        Long id,
        Long partidoId,
        Long usuarioId,
        Long equipeId,
        String funcao,
        boolean ativo,
        Instant criadoEm
) {
    public MembroEquipe {
        if (partidoId == null || usuarioId == null || equipeId == null) {
            throw new IllegalArgumentException("partido, usuário e equipe são obrigatórios");
        }
    }
}
