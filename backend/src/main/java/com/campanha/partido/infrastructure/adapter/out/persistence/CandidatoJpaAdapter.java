package com.campanha.partido.infrastructure.adapter.out.persistence;

import com.campanha.partido.application.port.out.CandidatoRepositoryPort;
import com.campanha.partido.domain.Candidato;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CandidatoJpaAdapter implements CandidatoRepositoryPort {

    private final CandidatoJpaRepository repo;

    @Override
    public Candidato save(Candidato c) {
        CandidatoJpaEntity entity = CandidatoJpaEntity.builder()
                .id(c.id())
                .partidoId(c.partidoId())
                .usuarioId(c.usuarioId())
                .nomeCompleto(c.nomeCompleto())
                .tituloEleitor(c.tituloEleitor())
                .numeroCandidato(c.numeroCandidato())
                .cargo(c.cargo())
                .uf(c.uf())
                .municipio(c.municipio())
                .criadoEm(c.criadoEm())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Candidato> findById(Long id) {
        return repo.findById(id).map(CandidatoJpaAdapter::toDomain);
    }

    @Override
    public List<Candidato> findAll() {
        return repo.findAll().stream().map(CandidatoJpaAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByTituloEleitorAndPartidoId(String titulo, Long partidoId) {
        return repo.existsByTituloEleitorAndPartidoId(titulo, partidoId);
    }

    static Candidato toDomain(CandidatoJpaEntity e) {
        return new Candidato(
                e.getId(),
                e.getPartidoId(),
                e.getUsuarioId(),
                e.getNomeCompleto(),
                e.getTituloEleitor(),
                e.getNumeroCandidato(),
                e.getCargo(),
                e.getUf(),
                e.getMunicipio(),
                e.getCriadoEm()
        );
    }
}
