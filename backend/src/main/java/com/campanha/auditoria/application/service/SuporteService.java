package com.campanha.auditoria.application.service;

import com.campanha.auditoria.application.port.in.SuporteUseCases;
import com.campanha.auditoria.application.port.out.SuporteRepositoryPort;
import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;
import com.campanha.auditoria.domain.StatusSolicitacaoSuporte;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Break-glass dual-control (D-09). Regras cruciais:
 *
 * <ul>
 *   <li>Solicitante deve ser SUPER_ADMIN.</li>
 *   <li>Aprovador deve ser SUPER_ADMIN <b>diferente</b> do solicitante.</li>
 *   <li>Fallback: se só houver 1 SUPER_ADMIN ativo, o ADMIN do partido alvo
 *       pode aprovar — marcado no log com {@code aprovacao_fallback = true}.</li>
 *   <li>Sessão de suporte expira automaticamente após 2h.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuporteService implements SuporteUseCases {

    static final Duration SESSAO_TTL = Duration.ofHours(2);

    private final SuporteRepositoryPort repo;

    @Override
    @Transactional
    public SolicitacaoAcessoSuporte abrirSolicitacao(AbrirCommand cmd) {
        if (!repo.isSuperAdmin(cmd.solicitanteId())) {
            throw new AccessDeniedException(
                    "apenas SUPER_ADMIN_PLATAFORMA pode abrir solicitação de suporte");
        }
        SolicitacaoAcessoSuporte nova = new SolicitacaoAcessoSuporte(
                null, cmd.solicitanteId(), cmd.partidoAlvoId(),
                cmd.motivo(), cmd.escopo(), Instant.now(),
                StatusSolicitacaoSuporte.PENDENTE, null, null, false, null, null);
        SolicitacaoAcessoSuporte salva = repo.save(nova);
        log.info("SUPORTE: solicitação {} aberta por usuario {} para partido {}",
                salva.id(), cmd.solicitanteId(), cmd.partidoAlvoId());
        return salva;
    }

    @Override
    @Transactional
    public SolicitacaoAcessoSuporte aprovar(Long solicitacaoId, Long aprovadorId) {
        SolicitacaoAcessoSuporte s = repo.findSolicitacao(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("solicitação não encontrada"));

        boolean aprovadorEhSuperAdmin = repo.isSuperAdmin(aprovadorId);
        boolean aprovadorEhAdminDoPartidoAlvo = repo.isAdminDoPartido(aprovadorId, s.partidoAlvoId());

        boolean fallback = false;
        if (aprovadorEhSuperAdmin) {
            if (aprovadorId.equals(s.solicitanteId())) {
                throw new AccessDeniedException(
                        "aprovador SUPER_ADMIN não pode ser o próprio solicitante (dual-control)");
            }
        } else if (aprovadorEhAdminDoPartidoAlvo) {
            long count = repo.contarSuperAdminsAtivos();
            if (count > 1) {
                throw new AccessDeniedException(
                        "fallback pelo ADMIN do partido alvo só é permitido quando há apenas 1 SUPER_ADMIN ativo (atual: "
                                + count + ")");
            }
            fallback = true;
        } else {
            throw new AccessDeniedException(
                    "aprovador deve ser SUPER_ADMIN diferente ou ADMIN do partido alvo (fallback)");
        }

        SolicitacaoAcessoSuporte aprovada = repo.save(s.aprovar(aprovadorId, fallback));
        log.info("SUPORTE: solicitação {} APROVADA por usuario {} (fallback={})",
                aprovada.id(), aprovadorId, fallback);
        return aprovada;
    }

    @Override
    @Transactional
    public SolicitacaoAcessoSuporte negar(Long solicitacaoId, Long aprovadorId, String motivo) {
        SolicitacaoAcessoSuporte s = repo.findSolicitacao(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("solicitação não encontrada"));
        return repo.save(s.negar(aprovadorId, motivo));
    }

    @Override
    @Transactional
    public IniciarSessaoResult iniciarSessao(Long solicitacaoId, Long usuarioId) {
        SolicitacaoAcessoSuporte s = repo.findSolicitacao(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("solicitação não encontrada"));
        if (!s.estaAprovada()) {
            throw new IllegalStateException(
                    "só é possível iniciar sessão para solicitação APROVADA (status atual: " + s.status() + ")");
        }
        if (!s.solicitanteId().equals(usuarioId)) {
            throw new AccessDeniedException(
                    "só o solicitante original pode iniciar a sessão de suporte");
        }
        String token = "SS-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Instant agora = Instant.now();
        AcessoSuporteLog log = new AcessoSuporteLog(
                null, solicitacaoId, usuarioId, s.partidoAlvoId(), s.escopo(),
                agora, agora.plus(SESSAO_TTL), null, token);
        AcessoSuporteLog salvo = repo.saveLog(log);
        SuporteService.log.info("SUPORTE: sessão iniciada usuario={} partido={} token=SS-... expira={}",
                usuarioId, s.partidoAlvoId(), salvo.expiraEm());
        return new IniciarSessaoResult(token, s.partidoAlvoId());
    }

    @Override
    @Transactional
    public void finalizarSessao(String tokenSessao) {
        AcessoSuporteLog log = repo.findLogPorToken(tokenSessao)
                .orElseThrow(() -> new IllegalArgumentException("token de sessão não encontrado"));
        repo.saveLog(log.finalizar());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitacaoAcessoSuporte> listarPendentes() { return repo.listarPendentes(); }

    @Override
    @Transactional(readOnly = true)
    public List<AcessoSuporteLog> listarLogs() { return repo.listarLogs(); }
}
