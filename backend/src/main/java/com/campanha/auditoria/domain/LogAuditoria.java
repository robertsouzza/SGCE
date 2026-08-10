package com.campanha.auditoria.domain;

import java.time.Instant;

public record LogAuditoria(
        Long id,
        Long usuarioId,
        String acao,
        String entidade,
        String entidadeId,
        String dadosAntes,
        String dadosDepois,
        Instant timestamp,
        String ip
) {}
