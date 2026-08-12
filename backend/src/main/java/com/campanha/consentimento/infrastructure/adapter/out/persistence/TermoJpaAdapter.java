package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import com.campanha.consentimento.application.port.out.TermoRepositoryPort;
import com.campanha.consentimento.domain.TermoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimentoMembro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TermoJpaAdapter implements TermoRepositoryPort {

    private final TermoConsentimentoJpaRepository eleitorRepo;
    private final TermoConsentimentoMembroJpaRepository membroRepo;

    @Override
    public TermoConsentimento save(TermoConsentimento t) {
        TermoConsentimentoJpaEntity e = TermoConsentimentoJpaEntity.builder()
                .id(t.id()).partidoId(t.partidoId()).versao(t.versao())
                .texto(t.texto()).vigenteAPartir(t.vigenteAPartir())
                .vigenteAte(t.vigenteAte()).criadoEm(t.criadoEm())
                .build();
        return toDomain(eleitorRepo.save(e));
    }

    @Override
    public Optional<TermoConsentimento> findById(Long id) {
        return eleitorRepo.findById(id).map(TermoJpaAdapter::toDomain);
    }

    @Override
    public Optional<TermoConsentimento> findVigenteMaisRecentePorPartido(Long partidoId) {
        Instant agora = Instant.now();
        return eleitorRepo.findFirstByPartidoIdOrderByVersaoDesc(partidoId)
                .map(TermoJpaAdapter::toDomain)
                .filter(t -> t.estaVigenteEm(agora));
    }

    @Override
    public List<TermoConsentimento> findAllByPartido(Long partidoId) {
        return eleitorRepo.findByPartidoIdOrderByVersaoDesc(partidoId).stream()
                .map(TermoJpaAdapter::toDomain).toList();
    }

    @Override
    public int proximaVersaoParaPartido(Long partidoId) {
        return eleitorRepo.maxVersao(partidoId) + 1;
    }

    @Override
    public TermoConsentimentoMembro saveMembro(TermoConsentimentoMembro t) {
        TermoConsentimentoMembroJpaEntity e = TermoConsentimentoMembroJpaEntity.builder()
                .id(t.id()).partidoId(t.partidoId()).versao(t.versao())
                .texto(t.texto()).vigenteAPartir(t.vigenteAPartir())
                .vigenteAte(t.vigenteAte()).criadoEm(t.criadoEm())
                .build();
        return toDomainMembro(membroRepo.save(e));
    }

    @Override
    public Optional<TermoConsentimentoMembro> findMembroById(Long id) {
        return membroRepo.findById(id).map(TermoJpaAdapter::toDomainMembro);
    }

    @Override
    public Optional<TermoConsentimentoMembro> findVigenteMembroMaisRecentePorPartido(Long partidoId) {
        return membroRepo.findFirstByPartidoIdOrderByVersaoDesc(partidoId)
                .map(TermoJpaAdapter::toDomainMembro);
    }

    @Override
    public int proximaVersaoMembroParaPartido(Long partidoId) {
        return membroRepo.maxVersao(partidoId) + 1;
    }

    static TermoConsentimento toDomain(TermoConsentimentoJpaEntity e) {
        return new TermoConsentimento(
                e.getId(), e.getPartidoId(), e.getVersao(), e.getTexto(),
                e.getVigenteAPartir(), e.getVigenteAte(), e.getCriadoEm());
    }

    static TermoConsentimentoMembro toDomainMembro(TermoConsentimentoMembroJpaEntity e) {
        return new TermoConsentimentoMembro(
                e.getId(), e.getPartidoId(), e.getVersao(), e.getTexto(),
                e.getVigenteAPartir(), e.getVigenteAte(), e.getCriadoEm());
    }
}
