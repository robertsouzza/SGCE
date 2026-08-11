package com.campanha.financeiro.infrastructure.adapter.out.persistence;

import com.campanha.financeiro.application.port.out.RecursoRepositoryPort;
import com.campanha.financeiro.domain.RecursoFundoEleitoral;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecursoJpaAdapter implements RecursoRepositoryPort {

    private final RecursoJpaRepository repo;

    @Override
    public RecursoFundoEleitoral save(RecursoFundoEleitoral r) {
        RecursoJpaEntity e = RecursoJpaEntity.builder()
                .id(r.id()).partidoId(r.partidoId()).candidatoId(r.candidatoId())
                .tipoRecurso(r.tipoRecurso()).valor(r.valor()).dataRepasse(r.dataRepasse())
                .origem(r.origem()).numeroDocumento(r.numeroDocumento())
                .comprovanteUrl(r.comprovanteUrl()).criadoEm(r.criadoEm())
                .build();
        return toDomain(repo.save(e));
    }

    @Override
    public Optional<RecursoFundoEleitoral> findById(Long id) {
        return repo.findById(id).map(RecursoJpaAdapter::toDomain);
    }

    @Override
    public List<RecursoFundoEleitoral> findByCandidatoId(Long candidatoId) {
        return repo.findByCandidatoId(candidatoId).stream().map(RecursoJpaAdapter::toDomain).toList();
    }

    @Override
    public BigDecimal totalPorCandidato(Long candidatoId) {
        return repo.totalPorCandidato(candidatoId);
    }

    @Override
    public List<RecursoFundoEleitoral> findAll() {
        return repo.findAll().stream().map(RecursoJpaAdapter::toDomain).toList();
    }

    static RecursoFundoEleitoral toDomain(RecursoJpaEntity e) {
        return new RecursoFundoEleitoral(
                e.getId(), e.getPartidoId(), e.getCandidatoId(),
                e.getTipoRecurso(), e.getValor(), e.getDataRepasse(),
                e.getOrigem(), e.getNumeroDocumento(),
                e.getComprovanteUrl(), e.getCriadoEm());
    }
}
