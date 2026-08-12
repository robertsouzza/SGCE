package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "consentimentos_membro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentimentoMembroJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(name = "termo_versao_id", nullable = false) private Long termoVersaoId;

    @Column(name = "consentimento_rastreamento", nullable = false)
    private boolean consentimentoRastreamentoConcedido;
    @Column(name = "consentimento_rastreamento_em")
    private Instant consentimentoRastreamentoEm;
    @Column(name = "consentimento_rastreamento_revogado", nullable = false)
    private boolean consentimentoRastreamentoRevogado;
    @Column(name = "consentimento_rastreamento_revogado_em")
    private Instant consentimentoRastreamentoRevogadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
