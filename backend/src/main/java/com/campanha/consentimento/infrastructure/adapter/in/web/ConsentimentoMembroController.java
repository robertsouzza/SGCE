package com.campanha.consentimento.infrastructure.adapter.in.web;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.consentimento.application.port.in.ConsentimentoUseCases;
import com.campanha.consentimento.domain.ConsentimentoMembro;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consentimentos-membro")
@RequiredArgsConstructor
public class ConsentimentoMembroController {

    private final ConsentimentoUseCases uc;

    /** Voluntário registra próprio consentimento — só ele mesmo. */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConsentimentoMembro> capturar(@RequestBody CapturarMembroRequest req,
                                                        Authentication auth) {
        return ResponseEntity.status(201).body(
                uc.capturarConsentimentoMembro(usuarioId(auth), req.concedido()));
    }

    @PostMapping("/revogar")
    @PreAuthorize("isAuthenticated()")
    public ConsentimentoMembro revogar(Authentication auth) {
        return uc.revogarConsentimentoMembro(usuarioId(auth));
    }

    private Long usuarioId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au.usuarioId();
    }

    public record CapturarMembroRequest(boolean concedido) {}
}
