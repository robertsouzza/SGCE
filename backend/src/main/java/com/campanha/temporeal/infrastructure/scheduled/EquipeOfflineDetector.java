package com.campanha.temporeal.infrastructure.scheduled;

import com.campanha.temporeal.application.port.out.LocalizacaoPublisherPort;
import com.campanha.temporeal.application.port.out.LocalizacaoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RF-20 (estado 3): a cada 30s varre membros ativos cujo último heartbeat
 * passou de 2min e publica evento "membro_offline_coletando" — o frontend
 * pinta a região do último ponto com cor distinta "Equipe em campo — sem
 * conexão".
 *
 * <p>Implementação de MVP: varre a tabela de histórico. Em produção,
 * cache TTL no Redis é mais eficiente (fica para skill futura).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EquipeOfflineDetector {

    private final LocalizacaoRepositoryPort repo;
    private final LocalizacaoPublisherPort publisher;

    /** Corte fixo em 2min — MVP. */
    static final Duration CORTE = Duration.ofMinutes(2);

    /** Roda a cada 30s. */
    @Scheduled(fixedDelayString = "30000")
    public void detectar() {
        // MVP: não temos hoje um índice de partidos ativos; a varredura real
        // será feita quando houver um cache de "partidos com membros em modo
        // campo" no Redis. Por ora este método loga apenas — hook em produção
        // deve iterar sobre partidos ativos.
        log.debug("EquipeOfflineDetector tick ({}s corte)", CORTE.toSeconds());
    }

    /**
     * API interna chamada pelo próprio serviço quando quiser varrer um
     * partido específico (ex: no fim de cada heartbeat processado).
     */
    public void varrerPartido(Long partidoId) {
        Instant corte = Instant.now().minus(CORTE);
        List<Long> offline = repo.membrosSemHeartbeatDesde(partidoId, corte);
        for (Long membroId : offline) {
            publisher.publicarEvento(partidoId, "membro_offline_coletando",
                    Map.of("membroId", membroId));
        }
    }
}
