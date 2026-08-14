package com.campanha.temporeal.application.port.out;

import com.campanha.temporeal.domain.LocalizacaoEquipe;

public interface LocalizacaoPublisherPort {
    /** Publica evento de localização no canal do partido (Redis pub/sub). */
    void publicarHeartbeat(LocalizacaoEquipe localizacao);

    /** Publica evento genérico (ex: abordagem_sincronizada, membro_offline_coletando). */
    void publicarEvento(Long partidoId, String tipo, Object payload);
}
