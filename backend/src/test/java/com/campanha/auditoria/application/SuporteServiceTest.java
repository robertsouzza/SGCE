package com.campanha.auditoria.application;

import com.campanha.auditoria.application.port.out.SuporteRepositoryPort;
import com.campanha.auditoria.application.service.SuporteService;
import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.auditoria.domain.SolicitacaoAcessoSuporte;
import com.campanha.auditoria.domain.StatusSolicitacaoSuporte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SuporteServiceTest {

    private SuporteRepositoryPort repo;
    private SuporteService service;

    @BeforeEach
    void setup() {
        repo = mock(SuporteRepositoryPort.class);
        service = new SuporteService(repo);
    }

    private SolicitacaoAcessoSuporte pendente(Long solicitanteId) {
        return new SolicitacaoAcessoSuporte(
                10L, solicitanteId, 42L, "motivo", "escopo",
                Instant.now(), StatusSolicitacaoSuporte.PENDENTE,
                null, null, false, null, null);
    }

    @Test
    void naoPermiteAbrirSeNaoForSuperAdmin() {
        when(repo.isSuperAdmin(100L)).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> service.abrirSolicitacao(new SuporteService.AbrirCommand(100L, 42L, "m", "e")));
    }

    @Test
    void aprovacaoPorOutroSuperAdminNaoEhFallback() {
        SolicitacaoAcessoSuporte s = pendente(100L);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(s));
        when(repo.isSuperAdmin(200L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitacaoAcessoSuporte r = service.aprovar(10L, 200L);
        assertEquals(StatusSolicitacaoSuporte.APROVADA, r.status());
        assertFalse(r.aprovacaoFallback(), "dois Super Admins → NÃO é fallback");
    }

    @Test
    void aprovacaoPorProprioSolicitanteBloqueiaMesmoSendoSuperAdmin() {
        SolicitacaoAcessoSuporte s = pendente(100L);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(s));
        when(repo.isSuperAdmin(100L)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.aprovar(10L, 100L));
    }

    @Test
    void fallbackSoPermitidoQuandoUnicoSuperAdmin() {
        SolicitacaoAcessoSuporte s = pendente(100L);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(s));
        when(repo.isSuperAdmin(300L)).thenReturn(false);
        when(repo.isAdminDoPartido(300L, 42L)).thenReturn(true);
        // Cenário 1: 2+ super admins → fallback proibido
        when(repo.contarSuperAdminsAtivos()).thenReturn(2L);
        assertThrows(AccessDeniedException.class, () -> service.aprovar(10L, 300L));

        // Cenário 2: 1 super admin → fallback permitido e marcado
        when(repo.contarSuperAdminsAtivos()).thenReturn(1L);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SolicitacaoAcessoSuporte r = service.aprovar(10L, 300L);
        assertTrue(r.aprovacaoFallback());
        assertEquals(300L, r.aprovadorId());
    }

    @Test
    void iniciarSessaoRejeitaSolicitacaoPendente() {
        SolicitacaoAcessoSuporte pendente = pendente(100L);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(pendente));
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSessao(10L, 100L));
    }

    @Test
    void iniciarSessaoRejeitaSeUsuarioDiferenteDoSolicitante() {
        SolicitacaoAcessoSuporte aprovada = pendente(100L).aprovar(200L, false);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(aprovada));
        assertThrows(AccessDeniedException.class,
                () -> service.iniciarSessao(10L, 999L));
    }

    @Test
    void iniciarSessaoAprovadaRetornaTokenEExpiraEm2h() {
        SolicitacaoAcessoSuporte aprovada = pendente(100L).aprovar(200L, false);
        when(repo.findSolicitacao(10L)).thenReturn(Optional.of(aprovada));
        when(repo.saveLog(any())).thenAnswer(inv -> {
            AcessoSuporteLog log = inv.getArgument(0);
            return new AcessoSuporteLog(999L, log.solicitacaoId(), log.usuarioId(),
                    log.partidoIdAcessado(), log.escopoAcesso(),
                    log.iniciadoEm(), log.expiraEm(), log.finalizadoEm(), log.tokenSessao());
        });

        SuporteService.IniciarSessaoResult r = service.iniciarSessao(10L, 100L);
        assertNotNull(r.tokenSessao());
        assertTrue(r.tokenSessao().startsWith("SS-"));
        assertEquals(42L, r.partidoAlvoId());
    }
}
