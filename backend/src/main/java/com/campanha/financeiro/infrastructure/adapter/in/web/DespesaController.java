package com.campanha.financeiro.infrastructure.adapter.in.web;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.financeiro.application.port.in.FinanceiroUseCases;
import com.campanha.financeiro.domain.CategoriaDespesa;
import com.campanha.financeiro.domain.Despesa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
public class DespesaController {

    private final FinanceiroUseCases uc;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO')")
    public ResponseEntity<Despesa> lancar(@Valid @RequestBody LancarDespesaRequest req,
                                          Authentication auth) {
        Long lancadoPor = usuarioId(auth);
        Despesa d = uc.lancarDespesa(new FinanceiroUseCases.LancarDespesaCommand(
                req.candidatoId(), req.categoria(), req.subcategoriaTse(),
                req.valor(), req.data(), req.descricao(), lancadoPor));
        return ResponseEntity.created(URI.create("/api/despesas/" + d.id())).body(d);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO','CANDIDATO')")
    public List<Despesa> listar() {
        return uc.listarDespesas();
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO')")
    public Despesa aprovar(@PathVariable Long id, Authentication auth) {
        return uc.aprovarDespesa(id, usuarioId(auth));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO')")
    public Despesa rejeitar(@PathVariable Long id, @Valid @RequestBody RejeitarRequest req,
                            Authentication auth) {
        return uc.rejeitarDespesa(id, usuarioId(auth), req.motivo());
    }

    @PostMapping("/{id}/comprovante")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO')")
    public Despesa anexarComprovante(@PathVariable Long id,
                                     @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return uc.anexarComprovanteDespesa(id, arquivo.getInputStream(),
                arquivo.getSize(), arquivo.getContentType(), arquivo.getOriginalFilename());
    }

    @GetMapping("/{id}/comprovante-url")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO')")
    public Map<String, String> presignedUrl(@PathVariable Long id) {
        return Map.of("url", uc.presignedUrlComprovanteDespesa(id));
    }

    private Long usuarioId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au.usuarioId();
    }

    public record LancarDespesaRequest(
            @NotNull Long candidatoId,
            @NotNull CategoriaDespesa categoria,
            String subcategoriaTse,
            @NotNull BigDecimal valor,
            @NotNull LocalDate data,
            String descricao
    ) {}

    public record RejeitarRequest(@NotBlank String motivo) {}
}
