package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimentoLGPDJpaRepository extends JpaRepository<ConsentimentoLGPDJpaEntity, Long> {
    Optional<ConsentimentoLGPDJpaEntity> findByCod(String cod);
    List<ConsentimentoLGPDJpaEntity> findByEleitorId(Long eleitorId);
}
