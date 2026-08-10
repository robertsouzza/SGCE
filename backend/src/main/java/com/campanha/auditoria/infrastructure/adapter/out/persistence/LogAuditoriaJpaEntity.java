package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "logs_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private String acao;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id")
    private String entidadeId;

    @Column(name = "dados_antes")
    @JdbcTypeCode(SqlTypes.JSON)
    private String dadosAntes;

    @Column(name = "dados_depois")
    @JdbcTypeCode(SqlTypes.JSON)
    private String dadosDepois;

    @Column(nullable = false)
    private Instant timestamp;

    private String ip;
}
