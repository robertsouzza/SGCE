package com.campanha.partido.application.port.out;

import com.campanha.partido.domain.Partido;

import java.util.List;
import java.util.Optional;

public interface PartidoRepositoryPort {
    Partido save(Partido partido);
    Optional<Partido> findById(Long id);
    List<Partido> findAll();
    boolean existsBySigla(String sigla);
    boolean existsByCnpj(String cnpj);
}
