package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EleitorJpaRepository extends JpaRepository<EleitorJpaEntity, Long> {
    Optional<EleitorJpaEntity> findByTituloEleitorAndPartidoId(String titulo, Long partidoId);

    @Query(value = "SELECT COUNT(*) FROM eleitores e " +
            "JOIN regioes_eleitorais r ON ST_Contains(r.geometria, e.geolocalizacao) " +
            "WHERE r.id = :regiaoId", nativeQuery = true)
    long contarPorRegiao(Long regiaoId);
}
