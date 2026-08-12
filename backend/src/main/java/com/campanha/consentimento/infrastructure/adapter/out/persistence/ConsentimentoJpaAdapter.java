package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import com.campanha.consentimento.application.port.out.ConsentimentoRepositoryPort;
import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.ConsentimentoMembro;
import com.campanha.consentimento.domain.EstadoConsentimento;
import com.campanha.consentimento.domain.MetodoCaptura;
import com.campanha.eleitores.infrastructure.adapter.out.persistence.GeoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConsentimentoJpaAdapter implements ConsentimentoRepositoryPort {

    private final ConsentimentoLGPDJpaRepository lgpdRepo;
    private final ConsentimentoMembroJpaRepository membroRepo;

    @Override
    public ConsentimentoLGPD save(ConsentimentoLGPD c) {
        ConsentimentoLGPDJpaEntity e = ConsentimentoLGPDJpaEntity.builder()
                .id(c.id())
                .partidoId(c.partidoId())
                .eleitorId(c.eleitorId())
                .abordagemId(c.abordagemId())
                .termoVersaoId(c.termoVersaoId())
                .metodoCaptura(c.metodoCaptura().name())
                .assinaturaArquivoUrl(c.assinaturaArquivoUrl())
                .membroCapturaId(c.membroCapturaId())
                .geolocalizacao(GeoFactory.toJts(c.geolocalizacao()))
                .timestampLocal(c.timestampLocal())
                .timestampSincronizacao(c.timestampSincronizacao())
                .contatoSalvoConfirmado(c.contatoSalvoConfirmado())
                .cod(c.cod())
                .consentimentoDadosConcedido(c.consentimentoDados().concedido())
                .consentimentoDadosEm(c.consentimentoDados().concedidoEm())
                .consentimentoDadosRevogado(c.consentimentoDados().revogado())
                .consentimentoDadosRevogadoEm(c.consentimentoDados().revogadoEm())
                .consentimentoWhatsappMarketingConcedido(c.consentimentoWhatsappMarketing().concedido())
                .consentimentoWhatsappMarketingEm(c.consentimentoWhatsappMarketing().concedidoEm())
                .consentimentoWhatsappMarketingRevogado(c.consentimentoWhatsappMarketing().revogado())
                .consentimentoWhatsappMarketingRevogadoEm(c.consentimentoWhatsappMarketing().revogadoEm())
                .criadoEm(c.criadoEm())
                .build();
        return toDomain(lgpdRepo.save(e));
    }

    @Override
    public Optional<ConsentimentoLGPD> findById(Long id) {
        return lgpdRepo.findById(id).map(ConsentimentoJpaAdapter::toDomain);
    }

    @Override
    public Optional<ConsentimentoLGPD> findByCod(String cod) {
        return lgpdRepo.findByCod(cod).map(ConsentimentoJpaAdapter::toDomain);
    }

    @Override
    public List<ConsentimentoLGPD> findByEleitorId(Long eleitorId) {
        return lgpdRepo.findByEleitorId(eleitorId).stream().map(ConsentimentoJpaAdapter::toDomain).toList();
    }

    @Override
    public ConsentimentoMembro saveMembro(ConsentimentoMembro c) {
        ConsentimentoMembroJpaEntity e = ConsentimentoMembroJpaEntity.builder()
                .id(c.id()).partidoId(c.partidoId())
                .usuarioId(c.usuarioId()).termoVersaoId(c.termoVersaoId())
                .consentimentoRastreamentoConcedido(c.consentimentoRastreamento().concedido())
                .consentimentoRastreamentoEm(c.consentimentoRastreamento().concedidoEm())
                .consentimentoRastreamentoRevogado(c.consentimentoRastreamento().revogado())
                .consentimentoRastreamentoRevogadoEm(c.consentimentoRastreamento().revogadoEm())
                .criadoEm(c.criadoEm())
                .build();
        return toDomainMembro(membroRepo.save(e));
    }

    @Override
    public Optional<ConsentimentoMembro> findMembroById(Long id) {
        return membroRepo.findById(id).map(ConsentimentoJpaAdapter::toDomainMembro);
    }

    @Override
    public Optional<ConsentimentoMembro> findMembroPorUsuario(Long usuarioId) {
        return membroRepo.findFirstByUsuarioIdOrderByIdDesc(usuarioId)
                .map(ConsentimentoJpaAdapter::toDomainMembro);
    }

    static ConsentimentoLGPD toDomain(ConsentimentoLGPDJpaEntity e) {
        return new ConsentimentoLGPD(
                e.getId(), e.getPartidoId(), e.getEleitorId(), e.getAbordagemId(),
                e.getTermoVersaoId(), MetodoCaptura.valueOf(e.getMetodoCaptura()),
                e.getAssinaturaArquivoUrl(), e.getMembroCapturaId(),
                GeoFactory.toDomain(e.getGeolocalizacao()),
                e.getTimestampLocal(), e.getTimestampSincronizacao(),
                e.isContatoSalvoConfirmado(), e.getCod(),
                new EstadoConsentimento(e.isConsentimentoDadosConcedido(),
                        e.getConsentimentoDadosEm(),
                        e.isConsentimentoDadosRevogado(),
                        e.getConsentimentoDadosRevogadoEm()),
                new EstadoConsentimento(e.isConsentimentoWhatsappMarketingConcedido(),
                        e.getConsentimentoWhatsappMarketingEm(),
                        e.isConsentimentoWhatsappMarketingRevogado(),
                        e.getConsentimentoWhatsappMarketingRevogadoEm()),
                e.getCriadoEm());
    }

    static ConsentimentoMembro toDomainMembro(ConsentimentoMembroJpaEntity e) {
        return new ConsentimentoMembro(
                e.getId(), e.getPartidoId(), e.getUsuarioId(), e.getTermoVersaoId(),
                new EstadoConsentimento(e.isConsentimentoRastreamentoConcedido(),
                        e.getConsentimentoRastreamentoEm(),
                        e.isConsentimentoRastreamentoRevogado(),
                        e.getConsentimentoRastreamentoRevogadoEm()),
                e.getCriadoEm());
    }
}
