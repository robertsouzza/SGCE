package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcessoSuporteLogJpaRepository extends JpaRepository<AcessoSuporteLogJpaEntity, Long> {
    Optional<AcessoSuporteLogJpaEntity> findByTokenSessao(String tokenSessao);
}
