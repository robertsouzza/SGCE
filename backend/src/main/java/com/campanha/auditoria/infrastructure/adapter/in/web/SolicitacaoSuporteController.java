package com.campanha.auditoria.infrastructure.adapter.in.web;

import com.campanha.auditoria.application.port.in.SuporteUseCases;
import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;
import com.campanha.autenticacao.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/suporte")
@RequiredArgsConstructor
public class SolicitacaoSuporteController {

    private final SuporteUseCases uc;

    @PostMapping("/solicitacoes")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public ResponseEntity<SolicitacaoAcessoSuporte> abrir(@Valid @RequestBody AbrirRequest req,
                                                           Authentication auth) {
        Long solicitante = usuario(auth).usuarioId();
        SolicitacaoAcessoSuporte s = uc.abrirSolicitacao(new SuporteUseCases.AbrirCommand(
                solicitante, req.partidoAlvoId(), req.motivo(), req.escopo()));
        return ResponseEntity.created(URI.create("/api/suporte/solicitacoes/" + s.id())).body(s);
    }

    @GetMapping("/solicitacoes/pendentes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN_PLATAFORMA','ADMIN')")
    public List<SolicitacaoAcessoSuporte> listarPendentes() {
        return uc.listarPendentes();
    }

    @PatchMapping("/solicitacoes/{id}/aprovar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN_PLATAFORMA','ADMIN')")
    public SolicitacaoAcessoSuporte aprovar(@PathVariable Long id, Authentication auth) {
        return uc.aprovar(id, usuario(auth).usuarioId());
    }

    @PatchMapping("/solicitacoes/{id}/negar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN_PLATAFORMA','ADMIN')")
    public SolicitacaoAcessoSuporte negar(@PathVariable Long id, @Valid @RequestBody NegarRequest req,
                                          Authentication auth) {
        return uc.negar(id, usuario(auth).usuarioId(), req.motivo());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public List<AcessoSuporteLog> listarLogs() {
        return uc.listarLogs();
    }

    private AuthenticatedUser usuario(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au;
    }

    public record AbrirRequest(@NotNull Long partidoAlvoId, @NotBlank String motivo, @NotBlank String escopo) {}
    public record NegarRequest(@NotBlank String motivo) {}
}
