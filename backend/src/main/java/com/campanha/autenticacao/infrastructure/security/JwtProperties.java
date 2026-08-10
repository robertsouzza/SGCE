package com.campanha.autenticacao.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sgce.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private int accessTokenTtlMinutes;
    private int refreshTokenTtlDays;
    private String accessCookieName;
    private String refreshCookieName;
    private boolean cookieSecure;
}
