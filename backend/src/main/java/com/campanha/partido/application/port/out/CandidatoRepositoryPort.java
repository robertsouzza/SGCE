package com.campanha.partido.application.port.out;

import com.campanha.partido.domain.Candidato;

import java.util.List;
import java.util.Optional;

public interface CandidatoRepositoryPort {
    Candidato save(Candidato candidato);
    Optional<Candidato> findById(Long id);
    List<Candidato> findAll();
    boolean existsByTituloEleitorAndPartidoId(String tituloEleitor, Long partidoId);
}
