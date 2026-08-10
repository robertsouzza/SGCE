package com.campanha.shared.multitenancy;

/**
 * Contexto de tenant (partido) da requisição atual, propagado via thread-local.
 *
 * Populado por {@code TenantFilter} logo após a autenticação. Consumido por
 * {@code TenantAwareTransactionAspect}, que emite {@code SET LOCAL
 * app.current_partido_id} para as Row-Level Security policies do PostgreSQL
 * atuarem (ver decisão D-05).
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_PARTIDO_ID = new ThreadLocal<>();

    private TenantContext() {}

    /** {@code null} indica SUPER_ADMIN_PLATAFORMA sem sessão de suporte. */
    public static Long get() {
        return CURRENT_PARTIDO_ID.get();
    }

    public static void set(Long partidoId) {
        CURRENT_PARTIDO_ID.set(partidoId);
    }

    public static void clear() {
        CURRENT_PARTIDO_ID.remove();
    }
}
