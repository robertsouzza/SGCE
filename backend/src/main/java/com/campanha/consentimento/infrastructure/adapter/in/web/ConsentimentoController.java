package com.campanha.consentimento.infrastructure.adapter.in.web;

import com.campanha.consentimento.application.port.in.ConsentimentoUseCases;
import com.campanha.consentimento.application.port.in.ConsentimentoUseCases.DeepLinkOptInResult;
import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.MetodoCaptura;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.infrastructure.adapter.in.web.EleitorController.GeoLocalizacaoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/consentimentos")
@RequiredArgsConstructor
public class ConsentimentoController {

    private final ConsentimentoUseCases uc;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ResponseEntity<ConsentimentoLGPD> capturar(@Valid @RequestBody CapturarRequest req) {
        Ponto geo = req.geolocalizacao() == null ? null
                : new Ponto(req.geolocalizacao().longitude(), req.geolocalizacao().latitude());
        ConsentimentoLGPD c = uc.capturarConsentimento(new ConsentimentoUseCases.CapturarConsentimentoCommand(
                req.eleitorId(), req.abordagemId(), req.metodoCaptura(),
                geo, req.timestampLocal(),
                req.consentimentoDados(), req.consentimentoWhatsappMarketing()));
        return ResponseEntity.created(URI.create("/api/consentimentos/" + c.id())).body(c);
    }

    @PostMapping("/{id}/assinatura")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ConsentimentoLGPD anexarAssinatura(@PathVariable Long id,
                                              @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return uc.anexarAssinatura(id, arquivo.getInputStream(),
                arquivo.getSize(), arquivo.getOriginalFilename());
    }

    @PostMapping("/{id}/revogar-dados")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ConsentimentoLGPD revogarDados(@PathVariable Long id) {
        return uc.revogarConsentimentoDados(id);
    }

    @PostMapping("/{id}/revogar-whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ConsentimentoLGPD revogarWhatsapp(@PathVariable Long id) {
        return uc.revogarConsentimentoWhatsApp(id);
    }

    @GetMapping("/deep-link-opt-in")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public DeepLinkOptInResult deepLink(@RequestParam Long abordagemId,
                                        @RequestParam Long candidatoId) {
        return uc.gerarDeepLinkOptIn(abordagemId, candidatoId);
    }

    public record CapturarRequest(
            @NotNull Long eleitorId,
            Long abordagemId,
            @NotNull MetodoCaptura metodoCaptura,
            GeoLocalizacaoRequest geolocalizacao,
            Instant timestampLocal,
            boolean consentimentoDados,
            boolean consentimentoWhatsappMarketing
    ) {}
}
