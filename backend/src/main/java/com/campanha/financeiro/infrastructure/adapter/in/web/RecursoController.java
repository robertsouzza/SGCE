package com.campanha.financeiro.infrastructure.adapter.in.web;

import com.campanha.financeiro.application.port.in.FinanceiroUseCases;
import com.campanha.financeiro.application.service.FinanceiroService;
import com.campanha.financeiro.domain.RecursoFundoEleitoral;
import com.campanha.financeiro.domain.TipoRecurso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final FinanceiroUseCases uc;
    private final FinanceiroService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO')")
    public ResponseEntity<RecursoFundoEleitoral> criar(@Valid @RequestBody RegistrarRecursoRequest req) {
        RecursoFundoEleitoral r = uc.registrarRecurso(new FinanceiroUseCases.RegistrarRecursoCommand(
                req.candidatoId(), req.tipoRecurso(), req.valor(),
                req.dataRepasse(), req.origem(), req.numeroDocumento()));
        return ResponseEntity.created(URI.create("/api/recursos/" + r.id())).body(r);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO','CANDIDATO')")
    public List<RecursoFundoEleitoral> listar() {
        return uc.listarRecursos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO','CANDIDATO')")
    public ResponseEntity<RecursoFundoEleitoral> buscarPorId(@PathVariable Long id) {
        return service.buscarRecurso(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO')")
    public RecursoFundoEleitoral atualizar(@PathVariable Long id, @Valid @RequestBody RegistrarRecursoRequest req) {
        return service.atualizarRecurso(id, new FinanceiroService.AtualizarRecursoCommand(
                req.tipoRecurso(), req.valor(), req.dataRepasse(),
                req.origem(), req.numeroDocumento()));
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO')")
    public RecursoFundoEleitoral inativar(@PathVariable Long id) {
        return service.alterarAtivoRecurso(id, false);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO')")
    public RecursoFundoEleitoral reativar(@PathVariable Long id) {
        return service.alterarAtivoRecurso(id, true);
    }

    public record RegistrarRecursoRequest(
            @NotNull Long candidatoId,
            @NotNull TipoRecurso tipoRecurso,
            @NotNull BigDecimal valor,
            @NotNull LocalDate dataRepasse,
            String origem,
            String numeroDocumento
    ) {}
}
