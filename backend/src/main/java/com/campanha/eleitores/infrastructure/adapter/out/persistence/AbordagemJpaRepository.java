package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbordagemJpaRepository extends JpaRepository<AbordagemJpaEntity, Long> {
    List<AbordagemJpaEntity> findByEleitorId(Long eleitorId);
}
