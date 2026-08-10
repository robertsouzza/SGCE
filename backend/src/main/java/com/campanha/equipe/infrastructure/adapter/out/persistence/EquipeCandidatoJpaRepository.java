package com.campanha.equipe.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipeCandidatoJpaRepository extends JpaRepository<EquipeCandidatoJpaEntity, Long> {
    List<EquipeCandidatoJpaEntity> findByEquipeId(Long equipeId);
}
