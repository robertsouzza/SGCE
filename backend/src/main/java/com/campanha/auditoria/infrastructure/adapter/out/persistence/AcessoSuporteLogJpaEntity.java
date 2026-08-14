package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "acessos_suporte_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcessoSuporteLogJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitacao_id", nullable = false) private Long solicitacaoId;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(name = "partido_id_acessado", nullable = false) private Long partidoIdAcessado;
    @Column(name = "escopo_acesso", nullable = false, length = 300) private String escopoAcesso;
    @Column(name = "iniciado_em", nullable = false) private Instant iniciadoEm;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "finalizado_em") private Instant finalizadoEm;
    @Column(name = "token_sessao", nullable = false, unique = true, length = 80) private String tokenSessao;
}
