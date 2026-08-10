package com.campanha.autenticacao.infrastructure.security;

import com.campanha.autenticacao.domain.Perfil;
import com.campanha.autenticacao.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    static final String CLAIM_PARTIDO_ID = "pid";
    static final String CLAIM_PERFIL = "perfil";
    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESS = "access";
    static final String TIPO_REFRESH = "refresh";

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Usuario usuario) {
        return build(usuario, TIPO_ACCESS, Duration.ofMinutes(props.getAccessTokenTtlMinutes()));
    }

    public String generateRefreshToken(Usuario usuario) {
        return build(usuario, TIPO_REFRESH, Duration.ofDays(props.getRefreshTokenTtlDays()));
    }

    private String build(Usuario usuario, String tipo, Duration ttl) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_PARTIDO_ID, usuario.partidoId());
        claims.put(CLAIM_PERFIL, usuario.perfil().name());
        claims.put(CLAIM_TIPO, tipo);
        return Jwts.builder()
                .subject(String.valueOf(usuario.id()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claims(claims)
                .signWith(key())
                .compact();
    }

    public ParsedToken parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
        Claims c = jws.getPayload();
        Long partidoId = c.get(CLAIM_PARTIDO_ID) == null ? null
                : ((Number) c.get(CLAIM_PARTIDO_ID)).longValue();
        Perfil perfil = Perfil.valueOf((String) c.get(CLAIM_PERFIL));
        String tipo = (String) c.get(CLAIM_TIPO);
        return new ParsedToken(Long.parseLong(c.getSubject()), partidoId, perfil, tipo);
    }

    public record ParsedToken(Long usuarioId, Long partidoId, Perfil perfil, String tipo) {
        public boolean isAccess() { return TIPO_ACCESS.equals(tipo); }
        public boolean isRefresh() { return TIPO_REFRESH.equals(tipo); }
    }
}
