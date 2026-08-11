package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.domain.Eleitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EleitorJpaAdapter implements EleitorRepositoryPort {

    private final EleitorJpaRepository repo;

    @Override
    public Eleitor save(Eleitor e) {
        EleitorJpaEntity entity = EleitorJpaEntity.builder()
                .id(e.id()).partidoId(e.partidoId())
                .nomeCompleto(e.nomeCompleto()).endereco(e.endereco())
                .geolocalizacao(GeoFactory.toJts(e.geolocalizacao()))
                .telefoneWhatsapp(e.telefoneWhatsapp())
                .tituloEleitor(e.tituloEleitor())
                .tituloEleitorHash(e.tituloEleitorHash())
                .zonaEleitoral(e.zonaEleitoral())
                .secaoEleitoral(e.secaoEleitoral())
                .observacoes(e.observacoes())
                .anonimizado(e.anonimizado())
                .anonimizadoEm(e.anonimizadoEm())
                .criadoEm(e.criadoEm())
                .atualizadoEm(e.atualizadoEm())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Eleitor> findById(Long id) {
        return repo.findById(id).map(EleitorJpaAdapter::toDomain);
    }

    @Override
    public Optional<Eleitor> findByTituloEleitorAndPartidoId(String titulo, Long partidoId) {
        return repo.findByTituloEleitorAndPartidoId(titulo, partidoId).map(EleitorJpaAdapter::toDomain);
    }

    @Override
    public List<Eleitor> findAll() {
        return repo.findAll().stream().map(EleitorJpaAdapter::toDomain).toList();
    }

    @Override
    public long contarPorRegiao(Long regiaoId) {
        return repo.contarPorRegiao(regiaoId);
    }

    static Eleitor toDomain(EleitorJpaEntity e) {
        return new Eleitor(
                e.getId(), e.getPartidoId(),
                e.getNomeCompleto(), e.getEndereco(),
                GeoFactory.toDomain(e.getGeolocalizacao()),
                e.getTelefoneWhatsapp(), e.getTituloEleitor(),
                e.getTituloEleitorHash(), e.getZonaEleitoral(),
                e.getSecaoEleitoral(), e.getObservacoes(),
                e.isAnonimizado(), e.getAnonimizadoEm(),
                e.getCriadoEm(), e.getAtualizadoEm());
    }
}
