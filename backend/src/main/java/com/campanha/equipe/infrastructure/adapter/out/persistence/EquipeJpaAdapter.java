package com.campanha.equipe.infrastructure.adapter.out.persistence;

import com.campanha.equipe.application.port.out.EquipeRepositoryPort;
import com.campanha.equipe.domain.Equipe;
import com.campanha.equipe.domain.EquipeCandidato;
import com.campanha.equipe.domain.MembroEquipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EquipeJpaAdapter implements EquipeRepositoryPort {

    private final EquipeJpaRepository equipeRepo;
    private final MembroEquipeJpaRepository membroRepo;
    private final EquipeCandidatoJpaRepository vinculoRepo;

    @Override
    public Equipe save(Equipe e) {
        return toDomain(equipeRepo.save(EquipeJpaEntity.builder()
                .id(e.id()).partidoId(e.partidoId()).nome(e.nome())
                .liderId(e.liderId()).regiaoAtuacao(e.regiaoAtuacao())
                .criadoEm(e.criadoEm()).build()));
    }

    @Override
    public Optional<Equipe> findById(Long id) {
        return equipeRepo.findById(id).map(EquipeJpaAdapter::toDomain);
    }

    @Override
    public List<Equipe> findAll() {
        return equipeRepo.findAll().stream().map(EquipeJpaAdapter::toDomain).toList();
    }

    @Override
    public MembroEquipe saveMembro(MembroEquipe m) {
        MembroEquipeJpaEntity saved = membroRepo.save(MembroEquipeJpaEntity.builder()
                .id(m.id()).partidoId(m.partidoId()).usuarioId(m.usuarioId())
                .equipeId(m.equipeId()).funcao(m.funcao()).ativo(m.ativo())
                .criadoEm(m.criadoEm()).build());
        return toDomain(saved);
    }

    @Override
    public List<MembroEquipe> findMembrosPorEquipe(Long equipeId) {
        return membroRepo.findByEquipeId(equipeId).stream().map(EquipeJpaAdapter::toDomain).toList();
    }

    @Override
    public EquipeCandidato saveVinculo(EquipeCandidato v) {
        EquipeCandidatoJpaEntity saved = vinculoRepo.save(EquipeCandidatoJpaEntity.builder()
                .id(v.id()).partidoId(v.partidoId()).equipeId(v.equipeId())
                .candidatoId(v.candidatoId()).vigenteDesde(v.vigenteDesde())
                .vigenteAte(v.vigenteAte()).build());
        return toDomain(saved);
    }

    @Override
    public List<EquipeCandidato> findCandidatosPorEquipe(Long equipeId) {
        return vinculoRepo.findByEquipeId(equipeId).stream().map(EquipeJpaAdapter::toDomain).toList();
    }

    private static Equipe toDomain(EquipeJpaEntity e) {
        return new Equipe(e.getId(), e.getPartidoId(), e.getNome(), e.getLiderId(), e.getRegiaoAtuacao(), e.getCriadoEm());
    }

    private static MembroEquipe toDomain(MembroEquipeJpaEntity m) {
        return new MembroEquipe(m.getId(), m.getPartidoId(), m.getUsuarioId(), m.getEquipeId(),
                m.getFuncao(), m.isAtivo(), m.getCriadoEm());
    }

    private static EquipeCandidato toDomain(EquipeCandidatoJpaEntity v) {
        return new EquipeCandidato(v.getId(), v.getPartidoId(), v.getEquipeId(), v.getCandidatoId(),
                v.getVigenteDesde(), v.getVigenteAte());
    }
}
