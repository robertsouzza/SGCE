package com.campanha.consentimento.domain;

import java.time.Instant;

public record TermoConsentimentoMembro(
        Long id,
        Long partidoId,
        int versao,
        String texto,
        Instant vigenteAPartir,
        Instant vigenteAte,
        Instant criadoEm
) {
    public TermoConsentimentoMembro {
        if (partidoId == null) throw new IllegalArgumentException("partido é obrigatório");
        if (versao < 1) throw new IllegalArgumentException("versao >= 1");
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("texto obrigatório");
    }
}
