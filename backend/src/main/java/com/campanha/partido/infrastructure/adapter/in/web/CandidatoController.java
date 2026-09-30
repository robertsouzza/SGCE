package com.campanha.partido.infrastructure.adapter.in.web;

import com.campanha.partido.application.port.in.CadastrarCandidatoUseCase;
import com.campanha.partido.application.port.in.ListarCandidatosUseCase;
import com.campanha.partido.application.service.CandidatoService;
import com.campanha.partido.domain.Candidato;
import com.campanha.partido.domain.Cargo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/candidatos")
@RequiredArgsConstructor
public class CandidatoController {

    private final CadastrarCandidatoUseCase cadastrar;
    private final ListarCandidatosUseCase listar;
    private final CandidatoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<Candidato> criar(@Valid @RequestBody CadastrarCandidatoRequest req) {
        Candidato c = cadastrar.executar(new CadastrarCandidatoUseCase.CadastrarCandidatoCommand(
                req.partidoId(), req.usuarioId(), req.nomeCompleto(),
                req.tituloEleitor(), req.numeroCandidato(), req.cargo(),
                req.uf(), req.municipio()
        ));
        return ResponseEntity.created(URI.create("/api/candidatos/" + c.id())).body(c);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Candidato> listarTodos() {
        return listar.executar();
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public Candidato inativar(@PathVariable Long id) {
        return service.alterarAtivo(id, false);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public Candidato reativar(@PathVariable Long id) {
        return service.alterarAtivo(id, true);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Candidato> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN_PLATAFORMA')")
    public Candidato atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarCandidatoRequest req) {
        return service.atualizar(id, new com.campanha.partido.application.service.CandidatoService.AtualizarCandidatoCommand(
                req.nomeCompleto(), req.tituloEleitor(), req.numeroCandidato(),
                req.cargo(), req.uf(), req.municipio()));
    }

    public record AtualizarCandidatoRequest(
            @NotBlank String nomeCompleto,
            @NotBlank String tituloEleitor,
            int numeroCandidato,
            @NotNull Cargo cargo,
            @NotBlank @Size(min = 2, max = 2) String uf,
            String municipio
    ) {}

    public record CadastrarCandidatoRequest(
            @NotNull Long partidoId,
            Long usuarioId,
            @NotBlank String nomeCompleto,
            @NotBlank String tituloEleitor,
            int numeroCandidato,
            @NotNull Cargo cargo,
            @NotBlank @Size(min = 2, max = 2) String uf,
            String municipio
    ) {}
}
