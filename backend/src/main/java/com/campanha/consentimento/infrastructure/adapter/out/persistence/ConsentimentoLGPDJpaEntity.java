package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "consentimentos_lgpd")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentimentoLGPDJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partido_id", nullable = false) private Long partidoId;
    @Column(name = "eleitor_id", nullable = false) private Long eleitorId;
    @Column(name = "abordagem_id") private Long abordagemId;
    @Column(name = "termo_versao_id", nullable = false) private Long termoVersaoId;

    @Column(name = "metodo_captura", nullable = false)
    private String metodoCaptura;

    @Column(name = "assinatura_arquivo_url")
    private String assinaturaArquivoUrl;

    @Column(name = "membro_captura_id", nullable = false)
    private Long membroCapturaId;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geolocalizacao;

    @Column(name = "timestamp_local")
    private Instant timestampLocal;

    @Column(name = "timestamp_sincronizacao", nullable = false)
    private Instant timestampSincronizacao;

    @Column(name = "contato_salvo_confirmado", nullable = false)
    private boolean contatoSalvoConfirmado;

    private String cod;

    // consentimento_dados
    @Column(name = "consentimento_dados", nullable = false)
    private boolean consentimentoDadosConcedido;
    @Column(name = "consentimento_dados_em")
    private Instant consentimentoDadosEm;
    @Column(name = "consentimento_dados_revogado", nullable = false)
    private boolean consentimentoDadosRevogado;
    @Column(name = "consentimento_dados_revogado_em")
    private Instant consentimentoDadosRevogadoEm;

    // consentimento_whatsapp_marketing
    @Column(name = "consentimento_whatsapp_marketing", nullable = false)
    private boolean consentimentoWhatsappMarketingConcedido;
    @Column(name = "consentimento_whatsapp_marketing_em")
    private Instant consentimentoWhatsappMarketingEm;
    @Column(name = "consentimento_whatsapp_marketing_revogado", nullable = false)
    private boolean consentimentoWhatsappMarketingRevogado;
    @Column(name = "consentimento_whatsapp_marketing_revogado_em")
    private Instant consentimentoWhatsappMarketingRevogadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
