package com.campanha.autenticacao.infrastructure.adapter.in.web;

import com.campanha.autenticacao.application.port.in.LoginUseCase;
import com.campanha.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.campanha.autenticacao.domain.Usuario;
import com.campanha.autenticacao.infrastructure.security.AuthenticatedUser;
import com.campanha.autenticacao.infrastructure.security.JwtProperties;
import com.campanha.autenticacao.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final UsuarioRepositoryPort usuarioRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties props;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                     HttpServletResponse response) {
        Usuario usuario = loginUseCase.autenticar(req.email(), req.senha());
        setAuthCookies(response, usuario);
        return ResponseEntity.ok(userBody(usuario));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request,
                                                       HttpServletResponse response) {
        String refresh = readCookie(request, props.getRefreshCookieName());
        if (refresh == null) {
            throw new BadCredentialsException("Refresh token ausente.");
        }
        JwtTokenProvider.ParsedToken parsed;
        try {
            parsed = tokenProvider.parse(refresh);
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token inválido.");
        }
        if (!parsed.isRefresh()) {
            throw new BadCredentialsException("Token não é refresh.");
        }
        Usuario usuario = usuarioRepository.findById(parsed.usuarioId())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado."));
        if (!usuario.ativo()) {
            throw new BadCredentialsException("Usuário inativo.");
        }
        setAuthCookies(response, usuario);
        return ResponseEntity.ok(userBody(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        expireCookie(response, props.getAccessCookieName());
        expireCookie(response, props.getRefreshCookieName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> csrfToken() {
        // O CookieCsrfTokenRepository já emite o XSRF-TOKEN em qualquer request.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException("Sem autenticação.");
        }
        Usuario usuario = usuarioRepository.findById(au.usuarioId())
                .orElseThrow(() -> new AccessDeniedException("Usuário do token não existe mais."));
        return ResponseEntity.ok(userBody(usuario));
    }

    private void setAuthCookies(HttpServletResponse response, Usuario usuario) {
        String access = tokenProvider.generateAccessToken(usuario);
        String refresh = tokenProvider.generateRefreshToken(usuario);
        response.addCookie(buildCookie(props.getAccessCookieName(), access,
                props.getAccessTokenTtlMinutes() * 60));
        response.addCookie(buildCookie(props.getRefreshCookieName(), refresh,
                props.getRefreshTokenTtlDays() * 24 * 60 * 60));
    }

    private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(props.isCookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    private void expireCookie(HttpServletResponse response, String name) {
        Cookie c = buildCookie(name, "", 0);
        response.addCookie(c);
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private Map<String, Object> userBody(Usuario u) {
        return Map.of(
                "id", u.id(),
                "nome", u.nome(),
                "email", u.email(),
                "perfil", u.perfil().name(),
                "partidoId", u.partidoId() == null ? "" : u.partidoId()
        );
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha) {}
}
