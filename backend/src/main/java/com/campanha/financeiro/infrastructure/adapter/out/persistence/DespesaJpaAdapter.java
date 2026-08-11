package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.application.port.out.DespesaRepositoryPort;
import com.campanha.financeiro.domain.Despesa;
import com.campanha.financeiro.domain.StatusDespesa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DespesaJpaAdapter implements DespesaRepositoryPort {

    private final DespesaJpaRepository repo;

    @Override
    public Despesa save(Despesa d) {
        DespesaJpaEntity e = DespesaJpaEntity.builder()
                .id(d.id()).partidoId(d.partidoId()).candidatoId(d.candidatoId())
                .categoria(d.categoria()).subcategoriaTse(d.subcategoriaTse())
                .valor(d.valor()).data(d.data()).descricao(d.descricao())
                .lancadoPor(d.lancadoPor()).comprovanteUrl(d.comprovanteUrl())
                .status(d.status()).aprovadoPor(d.aprovadoPor())
                .aprovadoEm(d.aprovadoEm()).motivoRejeicao(d.motivoRejeicao())
                .criadoEm(d.criadoEm())
                .build();
        return toDomain(repo.save(e));
    }

    @Override
    public Optional<Despesa> findById(Long id) {
        return repo.findById(id).map(DespesaJpaAdapter::toDomain);
    }

    @Override
    public List<Despesa> findByStatus(StatusDespesa status) {
        return repo.findByStatus(status).stream().map(DespesaJpaAdapter::toDomain).toList();
    }

    @Override
    public List<Despesa> findByCandidatoId(Long candidatoId) {
        return repo.findByCandidatoId(candidatoId).stream().map(DespesaJpaAdapter::toDomain).toList();
    }

    @Override
    public BigDecimal totalAprovadoPorCandidato(Long candidatoId) {
        return repo.totalAprovadoPorCandidato(candidatoId);
    }

    @Override
    public List<Despesa> findAll() {
        return repo.findAll().stream().map(DespesaJpaAdapter::toDomain).toList();
    }

    static Despesa toDomain(DespesaJpaEntity e) {
        return new Despesa(
                e.getId(), e.getPartidoId(), e.getCandidatoId(),
                e.getCategoria(), e.getSubcategoriaTse(), e.getValor(), e.getData(),
                e.getDescricao(), e.getLancadoPor(), e.getComprovanteUrl(),
                e.getStatus(), e.getAprovadoPor(), e.getAprovadoEm(),
                e.getMotivoRejeicao(), e.getCriadoEm());
    }
}
