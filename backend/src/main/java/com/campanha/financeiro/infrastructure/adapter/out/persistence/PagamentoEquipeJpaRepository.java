package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoEquipeJpaRepository extends JpaRepository<PagamentoEquipeJpaEntity, Long> {
    List<PagamentoEquipeJpaEntity> findByMembroId(Long membroId);
}
