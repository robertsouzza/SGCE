package com.campanha.partido.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidoJpaRepository extends JpaRepository<PartidoJpaEntity, Long> {
    boolean existsBySigla(String sigla);
    boolean existsByCnpj(String cnpj);
}
