package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntencaoVotoJpaRepository extends JpaRepository<IntencaoVotoJpaEntity, Long> {
    List<IntencaoVotoJpaEntity> findByAbordagemId(Long abordagemId);
}
