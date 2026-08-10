package com.campanha.auditoria.application.port.out;

import com.campanha.auditoria.domain.LogAuditoria;

public interface LogAuditoriaRepositoryPort {
    LogAuditoria save(LogAuditoria log);
}
