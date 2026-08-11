package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "intencoes_voto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntencaoVotoJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "abordagem_id", nullable = false) private Long abordagemId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;

    @Column(nullable = false)
    private String intencao;
}
