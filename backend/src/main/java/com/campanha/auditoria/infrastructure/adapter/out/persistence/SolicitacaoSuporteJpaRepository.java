package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoSuporteJpaRepository extends JpaRepository<SolicitacaoSuporteJpaEntity, Long> {
    List<SolicitacaoSuporteJpaEntity> findByStatusOrderByCriadaEmDesc(String status);
}
