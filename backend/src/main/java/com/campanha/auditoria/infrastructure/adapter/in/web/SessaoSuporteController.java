package com.campanha.auditoria.infrastructure.adapter.in.web;

import com.campanha.auditoria.application.port.in.SuporteUseCases;
import com.campanha.auditoria.application.port.in.SuporteUseCases.IniciarSessaoResult;
import com.campanha.autenticacao.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suporte/sessoes")
@RequiredArgsConstructor
public class SessaoSuporteController {

    private final SuporteUseCases uc;

    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public IniciarSessaoResult iniciar(@Valid @RequestBody IniciarRequest req, Authentication auth) {
        return uc.iniciarSessao(req.solicitacaoId(), usuario(auth).usuarioId());
    }

    @PostMapping("/finalizar")
    @PreAuthorize("hasRole('SUPER_ADMIN_PLATAFORMA')")
    public void finalizar(@Valid @RequestBody FinalizarRequest req) {
        uc.finalizarSessao(req.tokenSessao());
    }

    private AuthenticatedUser usuario(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au;
    }

    public record IniciarRequest(@NotNull Long solicitacaoId) {}
    public record FinalizarRequest(@NotBlank String tokenSessao) {}
}
