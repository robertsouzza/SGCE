package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaJpaRepository extends JpaRepository<LogAuditoriaJpaEntity, Long> {
}
