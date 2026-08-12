package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TermoConsentimentoMembroJpaRepository extends JpaRepository<TermoConsentimentoMembroJpaEntity, Long> {
    @Query("SELECT COALESCE(MAX(t.versao), 0) FROM TermoConsentimentoMembroJpaEntity t WHERE t.partidoId = :partidoId")
    int maxVersao(Long partidoId);

    Optional<TermoConsentimentoMembroJpaEntity> findFirstByPartidoIdOrderByVersaoDesc(Long partidoId);
}
