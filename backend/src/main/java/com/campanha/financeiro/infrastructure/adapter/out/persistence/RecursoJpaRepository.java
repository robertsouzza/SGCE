package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface RecursoJpaRepository extends JpaRepository<RecursoJpaEntity, Long> {
    List<RecursoJpaEntity> findByCandidatoId(Long candidatoId);

    @Query("SELECT COALESCE(SUM(r.valor), 0) FROM RecursoJpaEntity r WHERE r.candidatoId = :candidatoId")
    BigDecimal totalPorCandidato(Long candidatoId);
}
