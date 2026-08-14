package com.campanha.temporeal.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.infrastructure.adapter.out.persistence.GeoFactory;
import com.campanha.temporeal.application.port.out.LocalizacaoRepositoryPort;
import com.campanha.temporeal.domain.LocalizacaoEquipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalizacaoJpaAdapter implements LocalizacaoRepositoryPort {

    private final LocalizacaoJpaRepository repo;

    @Override
    public void gravarHistorico(LocalizacaoEquipe l) {
        repo.save(LocalizacaoJpaEntity.builder()
                .partidoId(l.partidoId())
                .membroId(l.membroId())
                .geolocalizacao(GeoFactory.toJts(l.ponto()))
                .timestamp(l.timestamp())
                .statusConexao(l.statusConexao().name())
                .build());
    }

    @Override
    public List<Long> membrosSemHeartbeatDesde(Long partidoId, Instant corte) {
        return repo.membrosSemHeartbeatDesde(partidoId, corte);
    }
}
