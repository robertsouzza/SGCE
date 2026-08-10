package com.campanha.equipe.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.equipe.application.port.in.EquipeUseCases;
import com.campanha.equipe.application.port.out.EquipeRepositoryPort;
import com.campanha.equipe.domain.Equipe;
import com.campanha.equipe.domain.EquipeCandidato;
import com.campanha.equipe.domain.MembroEquipe;
import com.campanha.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipeService implements EquipeUseCases {

    private final EquipeRepositoryPort repo;

    @Override
    @Transactional
    @Auditavel(acao = "cadastrar_equipe", entidade = "Equipe")
    public Equipe cadastrarEquipe(CadastrarEquipeCommand cmd) {
        Long partido = resolvePartido(cmd.partidoId());
        return repo.save(new Equipe(null, partido, cmd.nome(), cmd.liderId(), cmd.regiaoAtuacao(), Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipe> listarEquipes() {
        return repo.findAll();
    }

    @Override
    @Transactional
    @Auditavel(acao = "adicionar_membro", entidade = "MembroEquipe")
    public MembroEquipe adicionarMembro(AdicionarMembroCommand cmd) {
        Equipe equipe = repo.findById(cmd.equipeId())
                .orElseThrow(() -> new IllegalArgumentException("equipe não encontrada"));
        return repo.saveMembro(new MembroEquipe(
                null, equipe.partidoId(), cmd.usuarioId(), equipe.id(),
                cmd.funcao(), true, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "vincular_candidato_a_equipe", entidade = "EquipeCandidato")
    public EquipeCandidato vincularCandidato(VincularCandidatoCommand cmd) {
        Equipe equipe = repo.findById(cmd.equipeId())
                .orElseThrow(() -> new IllegalArgumentException("equipe não encontrada"));
        return repo.saveVinculo(new EquipeCandidato(
                null, equipe.partidoId(), equipe.id(), cmd.candidatoId(),
                cmd.vigenteDesde(), cmd.vigenteAte()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembroEquipe> listarMembros(Long equipeId) {
        return repo.findMembrosPorEquipe(equipeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipeCandidato> listarCandidatos(Long equipeId) {
        return repo.findCandidatosPorEquipe(equipeId);
    }

    private Long resolvePartido(Long solicitado) {
        Long atual = TenantContext.get();
        if (atual == null) {
            // SUPER_ADMIN: aceita o partido do comando
            if (solicitado == null) {
                throw new IllegalArgumentException("SUPER_ADMIN deve informar partidoId");
            }
            return solicitado;
        }
        if (solicitado != null && !solicitado.equals(atual)) {
            throw new AccessDeniedException(
                    "usuário do partido " + atual + " não pode operar sobre partido " + solicitado);
        }
        return atual;
    }
}
