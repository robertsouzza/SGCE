package com.campanha.consentimento.domain;

import java.time.Instant;

public record TermoConsentimento(
        Long id,
        Long partidoId,
        int versao,
        String texto,
        Instant vigenteAPartir,
        Instant vigenteAte,
        Instant criadoEm
) {
    public TermoConsentimento {
        if (partidoId == null) {
            throw new IllegalArgumentException("termo precisa de partido");
        }
        if (versao < 1) {
            throw new IllegalArgumentException("versao deve ser >= 1");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("texto do termo é obrigatório");
        }
    }

    public boolean estaVigenteEm(Instant momento) {
        boolean depoisDoInicio = vigenteAPartir == null || !momento.isBefore(vigenteAPartir);
        boolean antesDoFim = vigenteAte == null || momento.isBefore(vigenteAte);
        return depoisDoInicio && antesDoFim;
    }
}
