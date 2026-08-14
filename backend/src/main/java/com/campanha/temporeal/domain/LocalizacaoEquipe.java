package com.campanha.temporeal.domain;

import com.campanha.eleitores.domain.Ponto;

import java.time.Instant;

public record LocalizacaoEquipe(
        Long membroId,
        Long partidoId,
        Ponto ponto,
        Instant timestamp,
        StatusConexao statusConexao
) {
    public LocalizacaoEquipe {
        if (membroId == null || partidoId == null) {
            throw new IllegalArgumentException("membroId e partidoId são obrigatórios");
        }
        if (ponto == null) {
            throw new IllegalArgumentException("ponto é obrigatório");
        }
        if (statusConexao == null) {
            throw new IllegalArgumentException("statusConexao é obrigatório");
        }
    }
}
