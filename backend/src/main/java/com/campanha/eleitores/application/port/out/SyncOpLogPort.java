package com.campanha.eleitores.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Idempotência do endpoint de sincronização (D-04): armazena
 * client_op_id → server_entity_id para deduplicar reenvios do app offline.
 */
public interface SyncOpLogPort {
    Optional<Registrado> find(UUID clientOpId);
    void save(UUID clientOpId, Long partidoId, String entidade, Long serverEntityId, String status);

    record Registrado(UUID clientOpId, String entidade, Long serverEntityId, String status) {}
}
