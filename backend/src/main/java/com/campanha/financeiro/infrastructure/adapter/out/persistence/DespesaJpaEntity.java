package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.domain.CategoriaDespesa;
import com.campanha.financeiro.domain.StatusDespesa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "despesas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DespesaJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaDespesa categoria;

    @Column(name = "subcategoria_tse")
    private String subcategoriaTse;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    private String descricao;

    @Column(name = "lancado_por", nullable = false)
    private Long lancadoPor;

    @Column(name = "comprovante_url")
    private String comprovanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDespesa status;

    @Column(name = "aprovado_por")
    private Long aprovadoPor;

    @Column(name = "aprovado_em")
    private Instant aprovadoEm;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
