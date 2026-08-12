package com.campanha.consentimento.application.port.out;

import com.campanha.consentimento.domain.TermoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimentoMembro;

import java.util.List;
import java.util.Optional;

public interface TermoRepositoryPort {
    TermoConsentimento save(TermoConsentimento t);
    Optional<TermoConsentimento> findById(Long id);
    Optional<TermoConsentimento> findVigenteMaisRecentePorPartido(Long partidoId);
    List<TermoConsentimento> findAllByPartido(Long partidoId);
    int proximaVersaoParaPartido(Long partidoId);

    TermoConsentimentoMembro saveMembro(TermoConsentimentoMembro t);
    Optional<TermoConsentimentoMembro> findMembroById(Long id);
    Optional<TermoConsentimentoMembro> findVigenteMembroMaisRecentePorPartido(Long partidoId);
    int proximaVersaoMembroParaPartido(Long partidoId);
}
