package com.campanha.autenticacao.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Retorna 401 (não 403) quando request anônimo tenta endpoint autenticado.
 * Semanticamente correto: sem credencial → 401; com credencial mas sem
 * permissão → 403 (AccessDeniedHandler).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now() + "\"," +
                "\"status\":401," +
                "\"codigo\":\"NAO_AUTENTICADO\"," +
                "\"mensagem\":\"Autenticação requerida.\"}"
        );
    }
}
