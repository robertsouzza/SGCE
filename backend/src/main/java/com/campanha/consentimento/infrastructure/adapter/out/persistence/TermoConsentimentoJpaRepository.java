package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermoConsentimentoJpaRepository extends JpaRepository<TermoConsentimentoJpaEntity, Long> {
    List<TermoConsentimentoJpaEntity> findByPartidoIdOrderByVersaoDesc(Long partidoId);

    @Query("SELECT COALESCE(MAX(t.versao), 0) FROM TermoConsentimentoJpaEntity t WHERE t.partidoId = :partidoId")
    int maxVersao(Long partidoId);

    Optional<TermoConsentimentoJpaEntity> findFirstByPartidoIdOrderByVersaoDesc(Long partidoId);
}
