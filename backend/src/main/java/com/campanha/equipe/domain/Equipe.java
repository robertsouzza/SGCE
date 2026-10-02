package com.campanha.equipe.domain;

import java.time.Instant;

public record Equipe(
        Long id,
        Long partidoId,
        String nome,
        Long liderId,
        String regiaoAtuacao,
        boolean ativo,
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

    public Equipe comAtivo(boolean novoAtivo) {
        return new Equipe(id, partidoId, nome, liderId, regiaoAtuacao, novoAtivo, criadoEm);
    }
}
