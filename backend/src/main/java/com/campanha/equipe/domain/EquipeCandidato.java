package com.campanha.equipe.domain;

import java.time.LocalDate;

public record EquipeCandidato(
        Long id,
        Long partidoId,
        Long equipeId,
        Long candidatoId,
        LocalDate vigenteDesde,
        LocalDate vigenteAte
) {
    public EquipeCandidato {
        if (partidoId == null || equipeId == null || candidatoId == null) {
            throw new IllegalArgumentException("partido, equipe e candidato são obrigatórios");
        }
        if (vigenteDesde == null) {
            throw new IllegalArgumentException("vigenteDesde é obrigatório");
        }
        if (vigenteAte != null && vigenteAte.isBefore(vigenteDesde)) {
            throw new IllegalArgumentException("vigenteAte não pode ser antes de vigenteDesde");
        }
    }
}
