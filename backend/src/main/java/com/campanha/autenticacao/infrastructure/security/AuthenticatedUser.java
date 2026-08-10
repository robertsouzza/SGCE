package com.campanha.autenticacao.infrastructure.security;

import com.campanha.autenticacao.domain.Perfil;
import com.campanha.shared.multitenancy.TenantFilter;

/**
 * Principal populado pelo JwtCookieAuthenticationFilter. Implementa
 * {@link TenantFilter.AuthenticatedTenant} para que o TenantFilter possa
 * extrair o partido_id sem depender do módulo de autenticação diretamente.
 */
public record AuthenticatedUser(Long usuarioId, Long partidoId, Perfil perfil)
        implements TenantFilter.AuthenticatedTenant {
}
