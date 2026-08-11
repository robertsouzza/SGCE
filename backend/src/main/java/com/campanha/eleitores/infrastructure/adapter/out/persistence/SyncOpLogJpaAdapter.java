package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.application.port.out.SyncOpLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncOpLogJpaAdapter implements SyncOpLogPort {

    private final SyncOpLogJpaRepository repo;

    @Override
    public Optional<Registrado> find(UUID clientOpId) {
        return repo.findByClientOpId(clientOpId).map(e ->
                new Registrado(e.getClientOpId(), e.getEntidade(), e.getServerEntityId(), e.getStatus()));
    }

    @Override
    public void save(UUID clientOpId, Long partidoId, String entidade, Long serverEntityId, String status) {
        SyncOpLogJpaEntity e = SyncOpLogJpaEntity.builder()
                .clientOpId(clientOpId)
                .partidoId(partidoId)
                .entidade(entidade)
                .serverEntityId(serverEntityId)
                .status(status)
                .criadoEm(Instant.now())
                .build();
        repo.save(e);
    }
}
