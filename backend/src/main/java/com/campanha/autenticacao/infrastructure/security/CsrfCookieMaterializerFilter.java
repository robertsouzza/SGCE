package com.campanha.autenticacao.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Força a materialização do CsrfToken em cada request, para que o
 * CookieCsrfTokenRepository (com deferred loading do Spring Security 6)
 * de fato escreva o cookie XSRF-TOKEN na resposta. Sem este filtro, o
 * cookie não aparece até que algum código chame token.getToken().
 */
public class CsrfCookieMaterializerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
