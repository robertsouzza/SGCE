package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EleitorJpaRepository extends JpaRepository<EleitorJpaEntity, Long> {
    Optional<EleitorJpaEntity> findByTituloEleitorAndPartidoId(String titulo, Long partidoId);

    /**
     * Conta eleitores DISTINTOS abordados numa região (via geolocalização da
     * abordagem, não do eleitor). Isto sobrevive à anonimização (D-02): quando
     * o Eleitor é anonimizado sua geoloc some, mas a Abordagem preserva
     * geolocalizacao_abordagem, e o agregado por região continua coerente.
     */
    @Query(value = "SELECT COUNT(DISTINCT a.eleitor_id) FROM abordagens a " +
            "JOIN regioes_eleitorais r ON ST_Contains(r.geometria, a.geolocalizacao_abordagem) " +
            "WHERE r.id = :regiaoId", nativeQuery = true)
    long contarPorRegiao(Long regiaoId);
}
