package com.campanha.auditoria.domain;

import java.time.Instant;

/**
 * Máquina de estado do break-glass (D-09). SUPER_ADMIN abre solicitação;
 * outro SUPER_ADMIN (ou ADMIN do partido alvo em fallback) aprova;
 * só depois o solicitante pode iniciar a sessão de suporte.
 */
public record SolicitacaoAcessoSuporte(
        Long id,
        Long solicitanteId,
        Long partidoAlvoId,
        String motivo,
        String escopo,
        Instant criadaEm,
        StatusSolicitacaoSuporte status,
        Long aprovadorId,
        Instant aprovadaEm,
        boolean aprovacaoFallback,
        Instant negadaEm,
        String motivoNegacao
) {
    public SolicitacaoAcessoSuporte {
        if (solicitanteId == null || partidoAlvoId == null) {
            throw new IllegalArgumentException("solicitante e partido_alvo são obrigatórios");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("motivo é obrigatório (não pode ser vazio)");
        }
        if (escopo == null || escopo.isBlank()) {
            throw new IllegalArgumentException("escopo é obrigatório");
        }
        if (status == null) status = StatusSolicitacaoSuporte.PENDENTE;
    }

    public SolicitacaoAcessoSuporte aprovar(Long aprovadorId, boolean fallback) {
        if (status != StatusSolicitacaoSuporte.PENDENTE) {
            throw new IllegalStateException(
                    "só é possível aprovar solicitação PENDENTE (status atual: " + status + ")");
        }
        if (aprovadorId == null) {
            throw new IllegalArgumentException("aprovadorId é obrigatório");
        }
        if (aprovadorId.equals(solicitanteId)) {
            throw new IllegalArgumentException(
                    "aprovador não pode ser o próprio solicitante (dual-control)");
        }
        return new SolicitacaoAcessoSuporte(
                id, solicitanteId, partidoAlvoId, motivo, escopo, criadaEm,
                StatusSolicitacaoSuporte.APROVADA, aprovadorId, Instant.now(),
                fallback, null, null);
    }

    public SolicitacaoAcessoSuporte negar(Long aprovadorId, String motivoNegacao) {
        if (status != StatusSolicitacaoSuporte.PENDENTE) {
            throw new IllegalStateException(
                    "só é possível negar solicitação PENDENTE (status atual: " + status + ")");
        }
        if (motivoNegacao == null || motivoNegacao.isBlank()) {
            throw new IllegalArgumentException("motivo da negação é obrigatório");
        }
        return new SolicitacaoAcessoSuporte(
                id, solicitanteId, partidoAlvoId, motivo, escopo, criadaEm,
                StatusSolicitacaoSuporte.NEGADA, aprovadorId, null,
                false, Instant.now(), motivoNegacao);
    }

    public SolicitacaoAcessoSuporte finalizar() {
        return new SolicitacaoAcessoSuporte(
                id, solicitanteId, partidoAlvoId, motivo, escopo, criadaEm,
                StatusSolicitacaoSuporte.FINALIZADA, aprovadorId, aprovadaEm,
                aprovacaoFallback, negadaEm, motivoNegacao);
    }

    public boolean estaAprovada() { return status == StatusSolicitacaoSuporte.APROVADA; }
}
