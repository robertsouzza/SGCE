package com.campanha.equipe.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "equipe_candidato")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipeCandidatoJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "equipe_id", nullable = false) private Long equipeId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;
    @Column(name = "vigente_desde", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "vigente_ate") private LocalDate vigenteAte;
}
