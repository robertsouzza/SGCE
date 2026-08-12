package com.campanha.autenticacao.infrastructure.security;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extrai o JWT do cookie sgce_access e popula o SecurityContext com um
 * {@link AuthenticatedUser}. Sem cookie ou cookie inválido = requisição
 * segue anônima e cai no bloqueio do SecurityConfig.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = readAccessCookie(request);
        if (token != null) {
            try {
                JwtTokenProvider.ParsedToken parsed = tokenProvider.parse(token);
                if (parsed.isAccess()) {
                    AuthenticatedUser principal = new AuthenticatedUser(
                            parsed.usuarioId(), parsed.partidoId(), parsed.perfil());
                    var authority = new SimpleGrantedAuthority("ROLE_" + parsed.perfil().name());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("JWT inválido ou expirado: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String readAccessCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (props.getAccessCookieName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
