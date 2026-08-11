package com.campanha.eleitores.domain;

import java.time.Instant;
import java.util.List;

public record Abordagem(
        Long id,
        Long partidoId,
        Long eleitorId,
        Long membroId,
        Long equipeId,
        TipoAbordagem tipoAbordagem,
        Instant dataHora,
        Ponto geolocalizacaoAbordagem,
        Instant timestampLocal,
        Instant timestampSincronizacao,
        boolean sincronizado,
        List<IntencaoVoto> intencoes,
        Instant criadoEm
) {
    public Abordagem {
        if (partidoId == null || eleitorId == null || membroId == null) {
            throw new IllegalArgumentException("partido, eleitor e membro são obrigatórios");
        }
        if (tipoAbordagem == null) {
            throw new IllegalArgumentException("tipoAbordagem é obrigatório");
        }
        if (dataHora == null) {
            throw new IllegalArgumentException("dataHora é obrigatória");
        }
        intencoes = intencoes == null ? List.of() : List.copyOf(intencoes);
    }
}
