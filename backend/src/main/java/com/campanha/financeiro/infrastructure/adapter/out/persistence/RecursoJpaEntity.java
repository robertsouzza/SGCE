package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.domain.TipoRecurso;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recursos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecursoJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false)
    private TipoRecurso tipoRecurso;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_repasse", nullable = false)
    private LocalDate dataRepasse;

    private String origem;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "comprovante_url")
    private String comprovanteUrl;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
