package com.campanha.partido.infrastructure.adapter.in.web;

import com.campanha.partido.application.port.in.CadastrarPartidoUseCase;
import com.campanha.partido.application.port.in.ListarPartidosUseCase;
import com.campanha.partido.application.service.PartidoService;
import com.campanha.partido.domain.Partido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
public class PartidoController {

    private final CadastrarPartidoUseCase cadastrarPartido;
    private final ListarPartidosUseCase listarPartidos;
    private final PartidoService service;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<Partido> cadastrar(@Valid @RequestBody CadastrarPartidoRequest req) {
        Partido criado = cadastrarPartido.executar(new CadastrarPartidoUseCase.CadastrarPartidoCommand(
                req.nome(), req.sigla(), req.numeroPartido(), req.cnpj(),
                req.enderecoSede(), req.dadosBancariosContaPartidaria(),
                req.email(), req.telefone(), req.planoAssinatura()
        ));
        return ResponseEntity.created(URI.create("/api/partidos/" + criado.id())).body(criado);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Partido> listar() {
        return listarPartidos.executar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Partido> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public Partido atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarPartidoRequest req) {
        return service.atualizar(id, new PartidoService.AtualizarPartidoCommand(
                req.nome(), req.sigla(), req.numeroPartido(), req.cnpj(),
                req.enderecoSede(), req.dadosBancariosContaPartidaria(),
                req.email(), req.telefone(), req.planoAssinatura()
        ));
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public Partido inativar(@PathVariable Long id) {
        return service.alterarAtivo(id, false);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public Partido reativar(@PathVariable Long id) {
        return service.alterarAtivo(id, true);
    }

    public record CadastrarPartidoRequest(
            @NotBlank String nome,
            @NotBlank @Size(max = 20) String sigla,
            @Min(10) @Max(99) int numeroPartido,
            @NotBlank String cnpj,
            String enderecoSede,
            String dadosBancariosContaPartidaria,
            @Email String email,
            String telefone,
            String planoAssinatura
    ) {}

    public record AtualizarPartidoRequest(
            @NotBlank String nome,
            @NotBlank @Size(max = 20) String sigla,
            @Min(10) @Max(99) int numeroPartido,
            @NotBlank String cnpj,
            String enderecoSede,
            String dadosBancariosContaPartidaria,
            @Email String email,
            String telefone,
            String planoAssinatura
    ) {}
}
