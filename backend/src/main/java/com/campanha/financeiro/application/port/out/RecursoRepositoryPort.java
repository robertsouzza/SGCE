package com.campanha.financeiro.application.port.out;

import com.campanha.financeiro.domain.RecursoFundoEleitoral;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecursoRepositoryPort {
    RecursoFundoEleitoral save(RecursoFundoEleitoral r);
    Optional<RecursoFundoEleitoral> findById(Long id);
    List<RecursoFundoEleitoral> findByCandidatoId(Long candidatoId);
    BigDecimal totalPorCandidato(Long candidatoId);
    List<RecursoFundoEleitoral> findAll();
}
