package com.campanha.consentimento.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimentoMembroJpaRepository extends JpaRepository<ConsentimentoMembroJpaEntity, Long> {
    Optional<ConsentimentoMembroJpaEntity> findFirstByUsuarioIdOrderByIdDesc(Long usuarioId);
}
