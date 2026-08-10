package com.campanha.equipe.application.port.in;

import com.campanha.equipe.domain.Equipe;
import com.campanha.equipe.domain.EquipeCandidato;
import com.campanha.equipe.domain.MembroEquipe;

import java.time.LocalDate;
import java.util.List;

/** Facade de ports do módulo equipe. */
public interface EquipeUseCases {

    Equipe cadastrarEquipe(CadastrarEquipeCommand cmd);
    List<Equipe> listarEquipes();
    MembroEquipe adicionarMembro(AdicionarMembroCommand cmd);
    EquipeCandidato vincularCandidato(VincularCandidatoCommand cmd);
    List<MembroEquipe> listarMembros(Long equipeId);
    List<EquipeCandidato> listarCandidatos(Long equipeId);

    record CadastrarEquipeCommand(Long partidoId, String nome, Long liderId, String regiaoAtuacao) {}
    record AdicionarMembroCommand(Long equipeId, Long usuarioId, String funcao) {}
    record VincularCandidatoCommand(Long equipeId, Long candidatoId, LocalDate vigenteDesde, LocalDate vigenteAte) {}
}
