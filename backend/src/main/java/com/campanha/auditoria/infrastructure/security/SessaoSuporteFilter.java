package com.campanha.auditoria.infrastructure.security;

import com.campanha.auditoria.application.port.out.SuporteRepositoryPort;
import com.campanha.auditoria.domain.AcessoSuporteLog;
import com.campanha.autenticacao.domain.AuthenticatedUser;
import com.campanha.shared.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Se o request tem header X-Session-Suporte com token válido E o usuário
 * autenticado é o solicitante da sessão E o token não expirou → sobrepõe
 * o TenantContext para o partido alvo. Isso permite ao SUPER_ADMIN ler
 * dados de um partido específico via as mesmas rotas normais, sem quebrar
 * a RLS (o SET LOCAL app.current_partido_id vai apontar para o partido
 * alvo dentro da transação).
 *
 * <p>Ordem: DEVE rodar DEPOIS do TenantFilter (que popula o tenant a
 * partir do JWT). Como o TenantFilter tem @Order(100), este vai com
 * @Order(200).
 */
@Component
@Order(200)
@RequiredArgsConstructor
@Slf4j
public class SessaoSuporteFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Session-Suporte";
    private final SuporteRepositoryPort repo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        Optional<AcessoSuporteLog> logOpt = repo.findLogPorToken(token);
        if (logOpt.isEmpty()) {
            rejeitar(response, "SESSAO_SUPORTE_INVALIDA");
            return;
        }
        AcessoSuporteLog acesso = logOpt.get();
        if (acesso.estaExpirado(Instant.now())) {
            rejeitar(response, "SESSAO_SUPORTE_EXPIRADA");
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            rejeitar(response, "SESSAO_SUPORTE_SEM_AUTENTICACAO");
            return;
        }
        if (!acesso.usuarioId().equals(au.usuarioId())) {
            rejeitar(response, "SESSAO_SUPORTE_USUARIO_INCORRETO");
            return;
        }
        // Sobrepõe o TenantContext com o partido alvo — as queries nesta request
        // vão receber SET LOCAL app.current_partido_id = <partido_alvo>.
        TenantContext.set(acesso.partidoIdAcessado());
        log.info("SUPORTE: usuario={} atuando em partido={} via sessão {} — request {} {}",
                au.usuarioId(), acesso.partidoIdAcessado(), token.substring(0, Math.min(12, token.length())),
                request.getMethod(), request.getRequestURI());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void rejeitar(HttpServletResponse response, String codigo) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"codigo\":\"" + codigo + "\",\"mensagem\":\"" + codigo + "\"}");
    }
}
