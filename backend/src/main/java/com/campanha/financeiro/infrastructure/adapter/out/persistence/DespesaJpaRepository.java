package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.domain.StatusDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface DespesaJpaRepository extends JpaRepository<DespesaJpaEntity, Long> {
    List<DespesaJpaEntity> findByStatus(StatusDespesa status);
    List<DespesaJpaEntity> findByCandidatoId(Long candidatoId);

    @Query("SELECT COALESCE(SUM(d.valor), 0) FROM DespesaJpaEntity d " +
           "WHERE d.candidatoId = :candidatoId AND d.status = com.campanha.financeiro.domain.StatusDespesa.APROVADO")
    BigDecimal totalAprovadoPorCandidato(Long candidatoId);
}
