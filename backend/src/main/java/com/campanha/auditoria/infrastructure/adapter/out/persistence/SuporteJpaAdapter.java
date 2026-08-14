package com.campanha.auditoria.infrastructure.adapter.out.persistence;

import com.campanha.auditoria.application.port.out.SuporteRepositoryPort;
import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;
import com.campanha.auditoria.domain.StatusSolicitacaoSuporte;
import com.campanha.autenticacao.infrastructure.adapter.out.persistence.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SuporteJpaAdapter implements SuporteRepositoryPort {

    private final SolicitacaoSuporteJpaRepository solicRepo;
    private final AcessoSuporteLogJpaRepository logRepo;
    private final UsuarioJpaRepository usuarioRepo;

    @Override
    public SolicitacaoAcessoSuporte save(SolicitacaoAcessoSuporte s) {
        SolicitacaoSuporteJpaEntity e = SolicitacaoSuporteJpaEntity.builder()
                .id(s.id()).solicitanteId(s.solicitanteId()).partidoAlvoId(s.partidoAlvoId())
                .motivo(s.motivo()).escopo(s.escopo()).criadaEm(s.criadaEm())
                .status(s.status().name())
                .aprovadorId(s.aprovadorId()).aprovadaEm(s.aprovadaEm())
                .aprovacaoFallback(s.aprovacaoFallback())
                .negadaEm(s.negadaEm()).motivoNegacao(s.motivoNegacao())
                .build();
        return toDomain(solicRepo.save(e));
    }

    @Override
    public Optional<SolicitacaoAcessoSuporte> findSolicitacao(Long id) {
        return solicRepo.findById(id).map(SuporteJpaAdapter::toDomain);
    }

    @Override
    public List<SolicitacaoAcessoSuporte> listarPendentes() {
        return solicRepo.findByStatusOrderByCriadaEmDesc(StatusSolicitacaoSuporte.PENDENTE.name())
                .stream().map(SuporteJpaAdapter::toDomain).toList();
    }

    @Override
    public AcessoSuporteLog saveLog(AcessoSuporteLog l) {
        AcessoSuporteLogJpaEntity e = AcessoSuporteLogJpaEntity.builder()
                .id(l.id()).solicitacaoId(l.solicitacaoId()).usuarioId(l.usuarioId())
                .partidoIdAcessado(l.partidoIdAcessado()).escopoAcesso(l.escopoAcesso())
                .iniciadoEm(l.iniciadoEm()).expiraEm(l.expiraEm())
                .finalizadoEm(l.finalizadoEm()).tokenSessao(l.tokenSessao())
                .build();
        return toDomainLog(logRepo.save(e));
    }

    @Override
    public Optional<AcessoSuporteLog> findLogPorToken(String tokenSessao) {
        return logRepo.findByTokenSessao(tokenSessao).map(SuporteJpaAdapter::toDomainLog);
    }

    @Override
    public List<AcessoSuporteLog> listarLogs() {
        return logRepo.findAll().stream().map(SuporteJpaAdapter::toDomainLog).toList();
    }

    @Override
    public long contarSuperAdminsAtivos() {
        return usuarioRepo.findAll().stream()
                .filter(u -> "SUPER_ADMIN_PLATAFORMA".equals(u.getPerfil().name()))
                .filter(u -> u.isAtivo())
                .count();
    }

    @Override
    public boolean isSuperAdmin(Long usuarioId) {
        return usuarioRepo.findById(usuarioId)
                .map(u -> "SUPER_ADMIN_PLATAFORMA".equals(u.getPerfil().name()) && u.isAtivo())
                .orElse(false);
    }

    @Override
    public boolean isAdminDoPartido(Long usuarioId, Long partidoId) {
        return usuarioRepo.findById(usuarioId)
                .map(u -> u.isAtivo()
                        && "ADMIN".equals(u.getPerfil().name())
                        && partidoId.equals(u.getPartidoId()))
                .orElse(false);
    }

    static SolicitacaoAcessoSuporte toDomain(SolicitacaoSuporteJpaEntity e) {
        return new SolicitacaoAcessoSuporte(
                e.getId(), e.getSolicitanteId(), e.getPartidoAlvoId(),
                e.getMotivo(), e.getEscopo(), e.getCriadaEm(),
                StatusSolicitacaoSuporte.valueOf(e.getStatus()),
                e.getAprovadorId(), e.getAprovadaEm(), e.isAprovacaoFallback(),
                e.getNegadaEm(), e.getMotivoNegacao());
    }

    static AcessoSuporteLog toDomainLog(AcessoSuporteLogJpaEntity e) {
        return new AcessoSuporteLog(
                e.getId(), e.getSolicitacaoId(), e.getUsuarioId(),
                e.getPartidoIdAcessado(), e.getEscopoAcesso(),
                e.getIniciadoEm(), e.getExpiraEm(), e.getFinalizadoEm(),
                e.getTokenSessao());
    }
}
