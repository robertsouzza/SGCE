package com.campanha.partido.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatoJpaRepository extends JpaRepository<CandidatoJpaEntity, Long> {
    boolean existsByTituloEleitorAndPartidoId(String tituloEleitor, Long partidoId);
}
