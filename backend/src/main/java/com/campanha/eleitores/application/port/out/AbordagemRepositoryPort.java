package com.campanha.eleitores.application.port.out;

import com.campanha.eleitores.domain.Abordagem;

import java.util.List;
import java.util.Optional;

public interface AbordagemRepositoryPort {
    Abordagem save(Abordagem a);
    Optional<Abordagem> findById(Long id);
    List<Abordagem> findByEleitorId(Long eleitorId);
    List<Abordagem> findAll();
}
