package com.campanha.financeiro.application.port.out;

import com.campanha.financeiro.domain.Despesa;
import com.campanha.financeiro.domain.StatusDespesa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DespesaRepositoryPort {
    Despesa save(Despesa d);
    Optional<Despesa> findById(Long id);
    List<Despesa> findByStatus(StatusDespesa status);
    List<Despesa> findByCandidatoId(Long candidatoId);
    BigDecimal totalAprovadoPorCandidato(Long candidatoId);
    List<Despesa> findAll();
}
