package com.campanha.eleitores.infrastructure.adapter.in.web;

import com.campanha.autenticacao.infrastructure.security.AuthenticatedUser;
import com.campanha.eleitores.application.port.in.EleitoresUseCases;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.IntencaoInput;
import com.campanha.eleitores.domain.Abordagem;
import com.campanha.eleitores.domain.Intencao;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.TipoAbordagem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/abordagens")
@RequiredArgsConstructor
public class AbordagemController {

    private final EleitoresUseCases uc;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ResponseEntity<Abordagem> registrar(@Valid @RequestBody RegistrarAbordagemRequest req,
                                                Authentication auth) {
        Long membroId = usuarioId(auth);
        Ponto geo = req.geolocalizacao() == null ? null
                : new Ponto(req.geolocalizacao().longitude(), req.geolocalizacao().latitude());
        List<IntencaoInput> intencoes = req.intencoes() == null ? List.of()
                : req.intencoes().stream()
                    .map(i -> new IntencaoInput(i.candidatoId(), i.intencao()))
                    .toList();
        Abordagem a = uc.registrarAbordagem(new EleitoresUseCases.RegistrarAbordagemCommand(
                req.eleitorId(), req.equipeId(), membroId, req.tipoAbordagem(),
                req.dataHora() != null ? req.dataHora() : Instant.now(),
                geo, req.timestampLocal(), intencoes));
        return ResponseEntity.created(URI.create("/api/abordagens/" + a.id())).body(a);
    }

    private Long usuarioId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au.usuarioId();
    }

    public record RegistrarAbordagemRequest(
            @NotNull Long eleitorId,
            Long equipeId,
            @NotNull TipoAbordagem tipoAbordagem,
            Instant dataHora,
            EleitorController.GeoLocalizacaoRequest geolocalizacao,
            Instant timestampLocal,
            List<IntencaoRequest> intencoes
    ) {}

    public record IntencaoRequest(@NotNull Long candidatoId, @NotNull Intencao intencao) {}
}
