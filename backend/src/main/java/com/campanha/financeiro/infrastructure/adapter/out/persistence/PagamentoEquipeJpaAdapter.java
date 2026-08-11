package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.application.port.out.PagamentoEquipeRepositoryPort;
import com.campanha.financeiro.domain.PagamentoEquipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PagamentoEquipeJpaAdapter implements PagamentoEquipeRepositoryPort {

    private final PagamentoEquipeJpaRepository repo;

    @Override
    public PagamentoEquipe save(PagamentoEquipe p) {
        PagamentoEquipeJpaEntity e = PagamentoEquipeJpaEntity.builder()
                .id(p.id()).partidoId(p.partidoId()).despesaId(p.despesaId())
                .membroId(p.membroId()).tipoPagamento(p.tipoPagamento())
                .quantidade(p.quantidade()).valorUnitario(p.valorUnitario())
                .periodoReferencia(p.periodoReferencia()).criadoEm(p.criadoEm())
                .build();
        return toDomain(repo.save(e));
    }

    @Override
    public Optional<PagamentoEquipe> findById(Long id) {
        return repo.findById(id).map(PagamentoEquipeJpaAdapter::toDomain);
    }

    @Override
    public List<PagamentoEquipe> findByMembroId(Long membroId) {
        return repo.findByMembroId(membroId).stream().map(PagamentoEquipeJpaAdapter::toDomain).toList();
    }

    @Override
    public List<PagamentoEquipe> findAll() {
        return repo.findAll().stream().map(PagamentoEquipeJpaAdapter::toDomain).toList();
    }

    static PagamentoEquipe toDomain(PagamentoEquipeJpaEntity e) {
        return new PagamentoEquipe(
                e.getId(), e.getPartidoId(), e.getDespesaId(), e.getMembroId(),
                e.getTipoPagamento(), e.getQuantidade(), e.getValorUnitario(),
                e.getPeriodoReferencia(), e.getCriadoEm());
    }
}
