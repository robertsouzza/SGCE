package com.campanha.partido.infrastructure.adapter.out.persistence;

import com.campanha.partido.domain.Cargo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "candidatos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false)
    private Long partidoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "titulo_eleitor", nullable = false)
    private String tituloEleitor;

    @Column(name = "numero_candidato", nullable = false)
    private int numeroCandidato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @Column(nullable = false, length = 2)
    private String uf;

    private String municipio;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
