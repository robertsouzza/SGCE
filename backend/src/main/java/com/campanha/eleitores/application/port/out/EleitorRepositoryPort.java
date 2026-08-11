package com.campanha.eleitores.application.port.out;

import com.campanha.eleitores.domain.Eleitor;

import java.util.List;
import java.util.Optional;

public interface EleitorRepositoryPort {
    Eleitor save(Eleitor e);
    Optional<Eleitor> findById(Long id);
    Optional<Eleitor> findByTituloEleitorAndPartidoId(String titulo, Long partidoId);
    List<Eleitor> findAll();
    long contarPorRegiao(Long regiaoId);
}
