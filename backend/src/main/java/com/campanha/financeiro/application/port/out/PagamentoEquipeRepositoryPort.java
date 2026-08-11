package com.campanha.financeiro.application.port.out;

import com.campanha.financeiro.domain.PagamentoEquipe;

import java.util.List;
import java.util.Optional;

public interface PagamentoEquipeRepositoryPort {
    PagamentoEquipe save(PagamentoEquipe p);
    Optional<PagamentoEquipe> findById(Long id);
    List<PagamentoEquipe> findByMembroId(Long membroId);
    List<PagamentoEquipe> findAll();
}
