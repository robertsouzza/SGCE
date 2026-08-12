package com.campanha.autenticacao.domain;

import com.campanha.shared.multitenancy.TenantFilter;

/**
 * Principal do usuário autenticado — no domínio, para que use cases da
 * application possam identificar o autor da operação sem violar a fronteira
 * hexagonal. Implementa {@link TenantFilter.AuthenticatedTenant} (contrato
 * de shared/multitenancy, camada transversal) para que o TenantFilter possa
 * extrair o partido_id sem depender do módulo de autenticação diretamente.
 */
public record AuthenticatedUser(Long usuarioId, Long partidoId, Perfil perfil)
        implements TenantFilter.AuthenticatedTenant {
}
