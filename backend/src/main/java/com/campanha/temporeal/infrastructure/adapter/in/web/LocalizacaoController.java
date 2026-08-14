package com.campanha.temporeal.infrastructure.adapter.in.web;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.temporeal.application.port.in.TempoRealUseCases;
import com.campanha.temporeal.domain.LocalizacaoEquipe;
import com.campanha.temporeal.domain.StatusConexao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tempo-real")
@RequiredArgsConstructor
public class LocalizacaoController {

    private final TempoRealUseCases uc;

    @PostMapping("/heartbeat")
    @PreAuthorize("hasAnyRole('MEMBRO_EQUIPE','LIDER_EQUIPE')")
    public LocalizacaoEquipe heartbeat(@Valid @RequestBody HeartbeatRequest req, Authentication auth) {
        AuthenticatedUser au = usuario(auth);
        Ponto p = new Ponto(req.geolocalizacao().longitude(), req.geolocalizacao().latitude());
        StatusConexao status = req.statusConexao() != null ? req.statusConexao() : StatusConexao.ONLINE;
        return uc.registrarHeartbeat(new TempoRealUseCases.RegistrarHeartbeatCommand(
                au.usuarioId(), au.partidoId(), p, status));
    }

    private AuthenticatedUser usuario(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        if (au.partidoId() == null) {
            throw new AccessDeniedException("heartbeat exige membro de partido");
        }
        return au;
    }

    public record HeartbeatRequest(
            @NotNull GeoRequest geolocalizacao,
            StatusConexao statusConexao
    ) {}

    public record GeoRequest(double longitude, double latitude) {}
}
