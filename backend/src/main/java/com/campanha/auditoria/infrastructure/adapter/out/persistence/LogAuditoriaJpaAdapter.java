package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import com.campanha.auditoria.application.port.out.LogAuditoriaRepositoryPort;
import com.campanha.auditoria.domain.LogAuditoria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogAuditoriaJpaAdapter implements LogAuditoriaRepositoryPort {

    private final LogAuditoriaJpaRepository repo;

    @Override
    public LogAuditoria save(LogAuditoria log) {
        LogAuditoriaJpaEntity entity = LogAuditoriaJpaEntity.builder()
                .id(log.id())
                .usuarioId(log.usuarioId())
                .acao(log.acao())
                .entidade(log.entidade())
                .entidadeId(log.entidadeId())
                .dadosAntes(log.dadosAntes())
                .dadosDepois(log.dadosDepois())
                .timestamp(log.timestamp())
                .ip(log.ip())
                .build();
        LogAuditoriaJpaEntity saved = repo.save(entity);
        return new LogAuditoria(
                saved.getId(),
                saved.getUsuarioId(),
                saved.getAcao(),
                saved.getEntidade(),
                saved.getEntidadeId(),
                saved.getDadosAntes(),
                saved.getDadosDepois(),
                saved.getTimestamp(),
                saved.getIp()
        );
    }
}
