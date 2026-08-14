package com.campanha.auditoria.application.port.out;

import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;

import java.util.List;
import java.util.Optional;

public interface SuporteRepositoryPort {
    SolicitacaoAcessoSuporte save(SolicitacaoAcessoSuporte s);
    Optional<SolicitacaoAcessoSuporte> findSolicitacao(Long id);
    List<SolicitacaoAcessoSuporte> listarPendentes();

    AcessoSuporteLog saveLog(AcessoSuporteLog log);
    Optional<AcessoSuporteLog> findLogPorToken(String tokenSessao);
    List<AcessoSuporteLog> listarLogs();

    /** Perfil e existência do usuário — usado pelas regras de aprovação. */
    long contarSuperAdminsAtivos();
    boolean isSuperAdmin(Long usuarioId);
    boolean isAdminDoPartido(Long usuarioId, Long partidoId);
}
