package com.campanha.eleitores.domain;

/** VO — intenção de voto para um candidato dentro de uma abordagem (N:N). */
public record IntencaoVoto(
        Long id,
        Long partidoId,
        Long abordagemId,
        Long candidatoId,
        Intencao intencao
) {
    public IntencaoVoto {
        if (partidoId == null || candidatoId == null) {
            throw new IllegalArgumentException("partido e candidato são obrigatórios");
        }
        if (intencao == null) {
            throw new IllegalArgumentException("intencao é obrigatória");
        }
    }
}
