package com.campanha.partido.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "partidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartidoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String sigla;

    @Column(name = "numero_partido", nullable = false, unique = true)
    private int numeroPartido;

    @Column(nullable = false, unique = true)
    private String cnpj;

    @Column(name = "endereco_sede")
    private String enderecoSede;

    @Column(name = "dados_bancarios_conta_partidaria")
    private String dadosBancariosContaPartidaria;

    private String email;
    private String telefone;

    @Column(name = "plano_assinatura", nullable = false)
    private String planoAssinatura;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
