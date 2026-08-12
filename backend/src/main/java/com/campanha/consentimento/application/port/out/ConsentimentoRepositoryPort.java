package com.campanha.consentimento.application.port.out;

import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.ConsentimentoMembro;

import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepositoryPort {
    ConsentimentoLGPD save(ConsentimentoLGPD c);
    Optional<ConsentimentoLGPD> findById(Long id);
    Optional<ConsentimentoLGPD> findByCod(String cod);
    List<ConsentimentoLGPD> findByEleitorId(Long eleitorId);

    ConsentimentoMembro saveMembro(ConsentimentoMembro c);
    Optional<ConsentimentoMembro> findMembroById(Long id);
    Optional<ConsentimentoMembro> findMembroPorUsuario(Long usuarioId);
}
