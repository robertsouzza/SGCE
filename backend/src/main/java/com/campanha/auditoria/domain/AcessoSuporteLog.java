package com.campanha.auditoria.domain;

import java.time.Instant;

public record AcessoSuporteLog(
        Long id,
        Long solicitacaoId,
        Long usuarioId,
        Long partidoIdAcessado,
        String escopoAcesso,
        Instant iniciadoEm,
        Instant expiraEm,
        Instant finalizadoEm,
        String tokenSessao
) {
    public AcessoSuporteLog {
        if (solicitacaoId == null || usuarioId == null || partidoIdAcessado == null) {
            throw new IllegalArgumentException("solicitação, usuário e partido são obrigatórios");
        }
        if (tokenSessao == null || tokenSessao.isBlank()) {
            throw new IllegalArgumentException("tokenSessao é obrigatório");
        }
        if (expiraEm == null) {
            throw new IllegalArgumentException("expiraEm é obrigatório");
        }
    }

    public boolean estaExpirado(Instant agora) {
        return finalizadoEm != null || agora.isAfter(expiraEm);
    }

    public AcessoSuporteLog finalizar() {
        return new AcessoSuporteLog(id, solicitacaoId, usuarioId, partidoIdAcessado,
                escopoAcesso, iniciadoEm, expiraEm, Instant.now(), tokenSessao);
    }
}
