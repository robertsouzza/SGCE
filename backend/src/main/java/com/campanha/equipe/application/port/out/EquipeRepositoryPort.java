package com.campanha.equipe.application.port.out;

import com.campanha.equipe.domain.Equipe;
import com.campanha.equipe.domain.EquipeCandidato;
import com.campanha.equipe.domain.MembroEquipe;

import java.util.List;
import java.util.Optional;

public interface EquipeRepositoryPort {
    Equipe save(Equipe equipe);
    Optional<Equipe> findById(Long id);
    List<Equipe> findAll();

    MembroEquipe saveMembro(MembroEquipe membro);
    List<MembroEquipe> findMembrosPorEquipe(Long equipeId);

    EquipeCandidato saveVinculo(EquipeCandidato vinculo);
    List<EquipeCandidato> findCandidatosPorEquipe(Long equipeId);
}
