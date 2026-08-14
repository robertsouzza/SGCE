package com.campanha.temporeal.application.port.in;

import com.campanha.eleitores.domain.Ponto;
import com.campanha.temporeal.domain.LocalizacaoEquipe;
import com.campanha.temporeal.domain.StatusConexao;

public interface TempoRealUseCases {

    LocalizacaoEquipe registrarHeartbeat(RegistrarHeartbeatCommand cmd);

    void publicarAbordagemSincronizada(Long partidoId, Long abordagemId, Long regiaoId);

    record RegistrarHeartbeatCommand(
            Long membroId,
            Long partidoId,
            Ponto ponto,
            StatusConexao statusConexao
    ) {}
}
