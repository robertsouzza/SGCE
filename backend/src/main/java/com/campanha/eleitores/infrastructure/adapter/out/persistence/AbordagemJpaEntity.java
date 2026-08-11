package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "abordagens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AbordagemJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "eleitor_id", nullable = false) private Long eleitorId;
    @Column(name = "membro_id", nullable = false) private Long membroId;
    @Column(name = "equipe_id") private Long equipeId;

    @Column(name = "tipo_abordagem", nullable = false)
    private String tipoAbordagem;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(name = "geolocalizacao_abordagem", columnDefinition = "geometry(Point,4326)")
    private Point geolocalizacaoAbordagem;

    @Column(name = "timestamp_local")
    private Instant timestampLocal;

    @Column(name = "timestamp_sincronizacao", nullable = false)
    private Instant timestampSincronizacao;

    @Column(nullable = false)
    private boolean sincronizado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
