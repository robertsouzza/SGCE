package com.campanha.shared.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Popula o {@link TenantContext} com o partido_id do usuário autenticado.
 * Roda após o filtro de autenticação JWT — se não há autenticação, o contexto
 * fica vazio e as queries de negócio retornarão vazio (RLS bloqueia).
 */
@Component
@Order(TenantFilter.ORDER)
public class TenantFilter extends OncePerRequestFilter {

    static final int ORDER = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedTenant tenant) {
            TenantContext.set(tenant.partidoId());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** Contrato do principal populado pelo JwtCookieAuthenticationFilter. */
    public interface AuthenticatedTenant {
        Long partidoId();
    }
}
