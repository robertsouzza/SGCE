package com.campanha.consentimento.infrastructure.adapter.in.web;

import com.campanha.consentimento.application.port.in.ConsentimentoUseCases;
import com.campanha.consentimento.domain.TermoConsentimento;
import com.campanha.consentimento.domain.TermoConsentimentoMembro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class TermoController {

    private final ConsentimentoUseCases uc;

    @PostMapping("/api/termos-consentimento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TermoConsentimento> publicarEleitor(@Valid @RequestBody PublicarTermoRequest req) {
        TermoConsentimento t = uc.publicarTermoEleitor(req.texto());
        return ResponseEntity.created(URI.create("/api/termos-consentimento/" + t.id())).body(t);
    }

    @PostMapping("/api/termos-consentimento-membro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TermoConsentimentoMembro> publicarMembro(@Valid @RequestBody PublicarTermoRequest req) {
        TermoConsentimentoMembro t = uc.publicarTermoMembro(req.texto());
        return ResponseEntity.created(URI.create("/api/termos-consentimento-membro/" + t.id())).body(t);
    }

    public record PublicarTermoRequest(@NotBlank String texto) {}
}
