package com.campanha.equipe.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "membros_equipe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembroEquipeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(name = "equipe_id", nullable = false) private Long equipeId;
    private String funcao;
    @Column(nullable = false) private boolean ativo;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
}
