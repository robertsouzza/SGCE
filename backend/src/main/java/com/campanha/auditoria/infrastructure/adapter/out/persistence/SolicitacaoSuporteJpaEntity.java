package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "solicitacoes_acesso_suporte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitacaoSuporteJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitante_id", nullable = false) private Long solicitanteId;
    @Column(name = "partido_alvo_id", nullable = false) private Long partidoAlvoId;
    @Column(nullable = false, length = 500) private String motivo;
    @Column(nullable = false, length = 300) private String escopo;
    @Column(name = "criada_em", nullable = false) private Instant criadaEm;
    @Column(nullable = false) private String status;
    @Column(name = "aprovador_id") private Long aprovadorId;
    @Column(name = "aprovada_em") private Instant aprovadaEm;
    @Column(name = "aprovacao_fallback", nullable = false) private boolean aprovacaoFallback;
    @Column(name = "negada_em") private Instant negadaEm;
    @Column(name = "motivo_negacao", length = 500) private String motivoNegacao;
}
