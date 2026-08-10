package com.campanha.partido.infrastructure.adapter.out.persistence;

import com.campanha.partido.application.port.out.PartidoRepositoryPort;
import com.campanha.partido.domain.Partido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartidoJpaAdapter implements PartidoRepositoryPort {

    private final PartidoJpaRepository repo;

    @Override
    public Partido save(Partido p) {
        PartidoJpaEntity entity = PartidoJpaEntity.builder()
                .id(p.id())
                .nome(p.nome())
                .sigla(p.sigla())
                .numeroPartido(p.numeroPartido())
                .cnpj(p.cnpj())
                .enderecoSede(p.enderecoSede())
                .dadosBancariosContaPartidaria(p.dadosBancariosContaPartidaria())
                .email(p.email())
                .telefone(p.telefone())
                .planoAssinatura(p.planoAssinatura())
                .ativo(p.ativo())
                .criadoEm(p.criadoEm())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Partido> findById(Long id) {
        return repo.findById(id).map(PartidoJpaAdapter::toDomain);
    }

    @Override
    public List<Partido> findAll() {
        return repo.findAll().stream().map(PartidoJpaAdapter::toDomain).toList();
    }

    @Override
    public boolean existsBySigla(String sigla) { return repo.existsBySigla(sigla); }

    @Override
    public boolean existsByCnpj(String cnpj) { return repo.existsByCnpj(cnpj); }

    static Partido toDomain(PartidoJpaEntity e) {
        return new Partido(
                e.getId(),
                e.getNome(),
                e.getSigla(),
                e.getNumeroPartido(),
                e.getCnpj(),
                e.getEnderecoSede(),
                e.getDadosBancariosContaPartidaria(),
                e.getEmail(),
                e.getTelefone(),
                e.getPlanoAssinatura(),
                e.isAtivo(),
                e.getCriadoEm()
        );
    }
}
