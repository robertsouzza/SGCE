package com.campanha.eleitores.infrastructure.adapter.in.web;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.eleitores.application.port.in.EleitoresUseCases;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.OperacaoSync;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.ResultadoLoteSync;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Endpoint que recebe o batch do app offline. O cliente envia todas as
 * operações acumuladas (upsert de eleitor, cadastro de abordagem, etc.)
 * de uma vez. Resposta contém status por operação (D-04).
 */
@RestController
@RequestMapping("/api/sincronizacao")
@RequiredArgsConstructor
public class SincronizacaoController {

    private final EleitoresUseCases uc;

    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('MEMBRO_EQUIPE','LIDER_EQUIPE','ADMIN')")
    public ResultadoLoteSync sincronizar(@Valid @RequestBody LoteRequest req, Authentication auth) {
        Long membroId = usuarioId(auth);
        List<OperacaoSync> operacoes = req.operacoes().stream()
                .map(o -> new OperacaoSync(o.clientOpId(), o.entidade(), o.operacao(),
                        o.payload(), o.timestampLocal()))
                .toList();
        return uc.sincronizarLote(operacoes, membroId);
    }

    private Long usuarioId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("autenticação ausente");
        }
        return au.usuarioId();
    }

    public record LoteRequest(@NotEmpty List<OperacaoRequest> operacoes) {}

    public record OperacaoRequest(
            @NotNull UUID clientOpId,
            @NotNull String entidade,
            @NotNull String operacao,
            @NotNull Object payload,
            Instant timestampLocal
    ) {}
}
