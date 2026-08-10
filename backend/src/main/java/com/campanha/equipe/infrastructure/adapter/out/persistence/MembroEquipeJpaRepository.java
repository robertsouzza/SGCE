package com.campanha.equipe.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembroEquipeJpaRepository extends JpaRepository<MembroEquipeJpaEntity, Long> {
    List<MembroEquipeJpaEntity> findByEquipeId(Long equipeId);
}
