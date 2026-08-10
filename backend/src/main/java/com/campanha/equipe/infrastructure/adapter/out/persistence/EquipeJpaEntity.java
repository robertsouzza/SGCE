package com.campanha.equipe.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "equipes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(nullable = false) private String nome;
    @Column(name = "lider_id", nullable = false) private Long liderId;
    @Column(name = "regiao_atuacao") private String regiaoAtuacao;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
}
