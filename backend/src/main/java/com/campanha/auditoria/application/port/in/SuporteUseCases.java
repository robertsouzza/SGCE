package com.campanha.auditoria.application.port.in;

import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;

import java.util.List;

public interface SuporteUseCases {

    SolicitacaoAcessoSuporte abrirSolicitacao(AbrirCommand cmd);

    SolicitacaoAcessoSuporte aprovar(Long solicitacaoId, Long aprovadorId);

    SolicitacaoAcessoSuporte negar(Long solicitacaoId, Long aprovadorId, String motivo);

    IniciarSessaoResult iniciarSessao(Long solicitacaoId, Long usuarioId);

    void finalizarSessao(String tokenSessao);

    List<SolicitacaoAcessoSuporte> listarPendentes();
    List<AcessoSuporteLog> listarLogs();

    record AbrirCommand(Long solicitanteId, Long partidoAlvoId, String motivo, String escopo) {}

    record IniciarSessaoResult(String tokenSessao, Long partidoAlvoId) {}
}
