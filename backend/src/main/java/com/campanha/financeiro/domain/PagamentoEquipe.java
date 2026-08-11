package com.campanha.financeiro.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoEquipe(
        Long id,
        Long partidoId,
        Long despesaId,
        Long membroId,
        TipoPagamento tipoPagamento,
        int quantidade,
        BigDecimal valorUnitario,
        String periodoReferencia,
        Instant criadoEm
) {
    public PagamentoEquipe {
        if (partidoId == null || despesaId == null || membroId == null) {
            throw new IllegalArgumentException("partido, despesa e membro são obrigatórios");
        }
        if (tipoPagamento == null) {
            throw new IllegalArgumentException("tipoPagamento é obrigatório");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
        if (valorUnitario == null || valorUnitario.signum() <= 0) {
            throw new IllegalArgumentException("valorUnitario deve ser positivo");
        }
    }

    public BigDecimal total() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
