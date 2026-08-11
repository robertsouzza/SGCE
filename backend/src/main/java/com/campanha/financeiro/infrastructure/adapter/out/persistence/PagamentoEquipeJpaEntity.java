package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.domain.TipoPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagamentos_equipe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagamentoEquipeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "despesa_id", nullable = false) private Long despesaId;
    @Column(name = "membro_id", nullable = false) private Long membroId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false)
    private TipoPagamento tipoPagamento;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "periodo_referencia")
    private String periodoReferencia;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
