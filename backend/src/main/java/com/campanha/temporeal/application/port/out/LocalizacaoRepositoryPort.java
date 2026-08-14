package com.campanha.temporeal.application.port.out;

import com.campanha.temporeal.domain.LocalizacaoEquipe;

import java.time.Instant;
import java.util.List;

public interface LocalizacaoRepositoryPort {
    void gravarHistorico(LocalizacaoEquipe l);

    /** Membros do partido cujo último heartbeat foi antes de "corte". */
    List<Long> membrosSemHeartbeatDesde(Long partidoId, Instant corte);
}
