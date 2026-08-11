package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_op_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncOpLogJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "client_op_id", nullable = false, unique = true) private UUID clientOpId;
    @Column(nullable = false) private String entidade;
    @Column(name = "server_entity_id") private Long serverEntityId;
    @Column(nullable = false) private String status;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
}
