package com.campanha.temporeal.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "localizacao_equipe_tempo_real")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocalizacaoJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "membro_id", nullable = false) private Long membroId;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point geolocalizacao;

    @Column(nullable = false) private Instant timestamp;

    @Column(name = "status_conexao", nullable = false)
    private String statusConexao;
}
