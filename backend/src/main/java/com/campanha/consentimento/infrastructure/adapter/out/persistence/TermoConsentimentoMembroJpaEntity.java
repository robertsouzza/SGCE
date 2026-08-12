package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "termos_consentimento_membro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TermoConsentimentoMembroJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(nullable = false) private int versao;
    @Column(nullable = false, columnDefinition = "TEXT") private String texto;
    @Column(name = "vigente_a_partir", nullable = false) private Instant vigenteAPartir;
    @Column(name = "vigente_ate") private Instant vigenteAte;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
}
