package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SyncOpLogJpaRepository extends JpaRepository<SyncOpLogJpaEntity, Long> {
    Optional<SyncOpLogJpaEntity> findByClientOpId(UUID clientOpId);
}
