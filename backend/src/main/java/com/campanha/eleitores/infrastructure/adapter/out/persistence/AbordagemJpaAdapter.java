package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.application.port.out.AbordagemRepositoryPort;
import com.campanha.eleitores.domain.Abordagem;
import com.campanha.eleitores.domain.Intencao;
import com.campanha.eleitores.domain.IntencaoVoto;
import com.campanha.eleitores.domain.TipoAbordagem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AbordagemJpaAdapter implements AbordagemRepositoryPort {

    private final AbordagemJpaRepository abordagemRepo;
    private final IntencaoVotoJpaRepository intencaoRepo;

    @Override
    public Abordagem save(Abordagem a) {
        AbordagemJpaEntity e = AbordagemJpaEntity.builder()
                .id(a.id()).partidoId(a.partidoId())
                .eleitorId(a.eleitorId()).membroId(a.membroId())
                .equipeId(a.equipeId())
                .tipoAbordagem(a.tipoAbordagem().name())
                .dataHora(a.dataHora())
                .geolocalizacaoAbordagem(GeoFactory.toJts(a.geolocalizacaoAbordagem()))
                .timestampLocal(a.timestampLocal())
                .timestampSincronizacao(a.timestampSincronizacao())
                .sincronizado(a.sincronizado())
                .criadoEm(a.criadoEm())
                .build();
        AbordagemJpaEntity salvo = abordagemRepo.save(e);

        List<IntencaoVoto> intencoesPersistidas = new java.util.ArrayList<>();
        for (IntencaoVoto iv : a.intencoes()) {
            IntencaoVotoJpaEntity ent = IntencaoVotoJpaEntity.builder()
                    .id(iv.id())
                    .partidoId(iv.partidoId())
                    .abordagemId(salvo.getId())
                    .candidatoId(iv.candidatoId())
                    .intencao(iv.intencao().name())
                    .build();
            IntencaoVotoJpaEntity iSaved = intencaoRepo.save(ent);
            intencoesPersistidas.add(new IntencaoVoto(
                    iSaved.getId(), iSaved.getPartidoId(),
                    iSaved.getAbordagemId(), iSaved.getCandidatoId(),
                    Intencao.valueOf(iSaved.getIntencao())));
        }

        return toDomain(salvo, intencoesPersistidas);
    }

    @Override
    public Optional<Abordagem> findById(Long id) {
        return abordagemRepo.findById(id).map(e -> toDomain(e, carregarIntencoes(e.getId())));
    }

    @Override
    public List<Abordagem> findByEleitorId(Long eleitorId) {
        return abordagemRepo.findByEleitorId(eleitorId).stream()
                .map(e -> toDomain(e, carregarIntencoes(e.getId())))
                .toList();
    }

    @Override
    public List<Abordagem> findAll() {
        return abordagemRepo.findAll().stream()
                .map(e -> toDomain(e, carregarIntencoes(e.getId())))
                .toList();
    }

    private List<IntencaoVoto> carregarIntencoes(Long abordagemId) {
        return intencaoRepo.findByAbordagemId(abordagemId).stream()
                .map(iv -> new IntencaoVoto(iv.getId(), iv.getPartidoId(),
                        iv.getAbordagemId(), iv.getCandidatoId(),
                        Intencao.valueOf(iv.getIntencao())))
                .toList();
    }

    private static Abordagem toDomain(AbordagemJpaEntity e, List<IntencaoVoto> intencoes) {
        return new Abordagem(
                e.getId(), e.getPartidoId(), e.getEleitorId(), e.getMembroId(),
                e.getEquipeId(), TipoAbordagem.valueOf(e.getTipoAbordagem()),
                e.getDataHora(), GeoFactory.toDomain(e.getGeolocalizacaoAbordagem()),
                e.getTimestampLocal(), e.getTimestampSincronizacao(),
                e.isSincronizado(), intencoes, e.getCriadoEm());
    }
}
