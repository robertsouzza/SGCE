package com.campanha.financeiro.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecursoFundoEleitoral(
        Long id,
        Long partidoId,
        Long candidatoId,
        TipoRecurso tipoRecurso,
        BigDecimal valor,
        LocalDate dataRepasse,
        String origem,
        String numeroDocumento,
        String comprovanteUrl,
        Instant criadoEm
) {
    public RecursoFundoEleitoral {
        if (partidoId == null || candidatoId == null) {
            throw new IllegalArgumentException("recurso precisa de partido e candidato");
        }
        if (tipoRecurso == null) {
            throw new IllegalArgumentException("tipoRecurso é obrigatório");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        if (dataRepasse == null) {
            throw new IllegalArgumentException("dataRepasse é obrigatória");
        }
    }
}
