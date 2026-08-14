package com.campanha.temporeal.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface LocalizacaoJpaRepository extends JpaRepository<LocalizacaoJpaEntity, Long> {

    @Query(value = "SELECT DISTINCT membro_id FROM localizacao_equipe_tempo_real l1 " +
            "WHERE l1.partido_id = :partidoId " +
            "AND l1.timestamp = (SELECT MAX(l2.timestamp) FROM localizacao_equipe_tempo_real l2 " +
            "                    WHERE l2.membro_id = l1.membro_id) " +
            "AND l1.timestamp < :corte", nativeQuery = true)
    List<Long> membrosSemHeartbeatDesde(Long partidoId, Instant corte);
}
