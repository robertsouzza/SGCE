package com.campanha.equipe.infrastructure.adapter.in.web;

import com.campanha.equipe.application.port.in.EquipeUseCases;
import com.campanha.equipe.domain.Equipe;
import com.campanha.equipe.domain.EquipeCandidato;
import com.campanha.equipe.domain.MembroEquipe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/equipes")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeUseCases uc;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<Equipe> cadastrar(@Valid @RequestBody CadastrarEquipeRequest req) {
        Equipe e = uc.cadastrarEquipe(new EquipeUseCases.CadastrarEquipeCommand(
                req.partidoId(), req.nome(), req.liderId(), req.regiaoAtuacao()));
        return ResponseEntity.created(URI.create("/api/equipes/" + e.id())).body(e);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Equipe> listar() {
        return uc.listarEquipes();
    }

    @PostMapping("/{equipeId}/membros")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<MembroEquipe> adicionarMembro(@PathVariable Long equipeId,
                                                        @Valid @RequestBody AdicionarMembroRequest req) {
        return ResponseEntity.status(201).body(
                uc.adicionarMembro(new EquipeUseCases.AdicionarMembroCommand(equipeId, req.usuarioId(), req.funcao())));
    }

    @GetMapping("/{equipeId}/membros")
    @PreAuthorize("isAuthenticated()")
    public List<MembroEquipe> listarMembros(@PathVariable Long equipeId) {
        return uc.listarMembros(equipeId);
    }

    @PostMapping("/{equipeId}/candidatos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<EquipeCandidato> vincularCandidato(@PathVariable Long equipeId,
                                                             @Valid @RequestBody VincularCandidatoRequest req) {
        return ResponseEntity.status(201).body(
                uc.vincularCandidato(new EquipeUseCases.VincularCandidatoCommand(
                        equipeId, req.candidatoId(), req.vigenteDesde(), req.vigenteAte())));
    }

    @GetMapping("/{equipeId}/candidatos")
    @PreAuthorize("isAuthenticated()")
    public List<EquipeCandidato> listarCandidatos(@PathVariable Long equipeId) {
        return uc.listarCandidatos(equipeId);
    }

    public record CadastrarEquipeRequest(Long partidoId, @NotBlank String nome, @NotNull Long liderId, String regiaoAtuacao) {}
    public record AdicionarMembroRequest(@NotNull Long usuarioId, String funcao) {}
    public record VincularCandidatoRequest(@NotNull Long candidatoId, @NotNull LocalDate vigenteDesde, LocalDate vigenteAte) {}
}
