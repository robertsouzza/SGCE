package com.campanha.temporeal.application.service;

import com.campanha.consentimento.application.port.in.ConsentimentoUseCases;
import com.campanha.temporeal.application.port.in.TempoRealUseCases;
import com.campanha.temporeal.application.port.out.LocalizacaoPublisherPort;
import com.campanha.temporeal.application.port.out.LocalizacaoRepositoryPort;
import com.campanha.temporeal.domain.LocalizacaoEquipe;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TempoRealService implements TempoRealUseCases {

    private final LocalizacaoPublisherPort publisher;
    private final LocalizacaoRepositoryPort repo;
    private final ConsentimentoUseCases consentimentoUc;

    @Override
    @Transactional
    public LocalizacaoEquipe registrarHeartbeat(RegistrarHeartbeatCommand cmd) {
        // D-10: voluntário só pode ser rastreado com consentimento vigente
        if (!consentimentoUc.consentimentoRastreamentoAtivo(cmd.membroId())) {
            throw new AccessDeniedException("MODO_CAMPO_INATIVO");
        }
        LocalizacaoEquipe l = new LocalizacaoEquipe(
                cmd.membroId(), cmd.partidoId(), cmd.ponto(), Instant.now(), cmd.statusConexao());
        repo.gravarHistorico(l);
        publisher.publicarHeartbeat(l);
        return l;
    }

    @Override
    public void publicarAbordagemSincronizada(Long partidoId, Long abordagemId, Long regiaoId) {
        publisher.publicarEvento(partidoId, "abordagem_sincronizada",
                Map.of("abordagemId", abordagemId, "regiaoId", regiaoId));
    }
}
