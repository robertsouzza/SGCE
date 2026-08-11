package com.campanha.financeiro.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record Despesa(
        Long id,
        Long partidoId,
        Long candidatoId,
        CategoriaDespesa categoria,
        String subcategoriaTse,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        Long lancadoPor,
        String comprovanteUrl,
        StatusDespesa status,
        Long aprovadoPor,
        Instant aprovadoEm,
        String motivoRejeicao,
        Instant criadoEm
) {
    public Despesa {
        if (partidoId == null || candidatoId == null) {
            throw new IllegalArgumentException("despesa precisa de partido e candidato");
        }
        if (categoria == null) {
            throw new IllegalArgumentException("categoria é obrigatória");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        if (data == null) {
            throw new IllegalArgumentException("data é obrigatória");
        }
        if (lancadoPor == null) {
            throw new IllegalArgumentException("lancadoPor é obrigatório");
        }
        if (status == null) {
            throw new IllegalArgumentException("status é obrigatório");
        }
    }

    /** Marca como APROVADO. Lança se não estiver PENDENTE. */
    public Despesa aprovar(Long usuarioId) {
        if (status != StatusDespesa.PENDENTE) {
            throw new IllegalStateException(
                    "só é possível aprovar despesa PENDENTE (status atual: " + status + ")");
        }
        return new Despesa(id, partidoId, candidatoId, categoria, subcategoriaTse, valor, data,
                descricao, lancadoPor, comprovanteUrl,
                StatusDespesa.APROVADO, usuarioId, Instant.now(), null, criadoEm);
    }

    /** Marca como REJEITADO. Lança se não estiver PENDENTE. */
    public Despesa rejeitar(Long usuarioId, String motivo) {
        if (status != StatusDespesa.PENDENTE) {
            throw new IllegalStateException(
                    "só é possível rejeitar despesa PENDENTE (status atual: " + status + ")");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("motivo da rejeição é obrigatório");
        }
        return new Despesa(id, partidoId, candidatoId, categoria, subcategoriaTse, valor, data,
                descricao, lancadoPor, comprovanteUrl,
                StatusDespesa.REJEITADO, usuarioId, Instant.now(), motivo, criadoEm);
    }

    public Despesa comComprovante(String url) {
        return new Despesa(id, partidoId, candidatoId, categoria, subcategoriaTse, valor, data,
                descricao, lancadoPor, url, status, aprovadoPor, aprovadoEm, motivoRejeicao, criadoEm);
    }
}
