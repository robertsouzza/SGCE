package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "eleitores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EleitorJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "nome_completo", nullable = false) private String nomeCompleto;
    private String endereco;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geolocalizacao;

    @Column(name = "telefone_whatsapp") private String telefoneWhatsapp;
    @Column(name = "titulo_eleitor") private String tituloEleitor;
    @Column(name = "titulo_eleitor_hash") private String tituloEleitorHash;
    @Column(name = "zona_eleitoral") private String zonaEleitoral;
    @Column(name = "secao_eleitoral") private String secaoEleitoral;
    private String observacoes;

    @Column(nullable = false)
    private boolean anonimizado;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
