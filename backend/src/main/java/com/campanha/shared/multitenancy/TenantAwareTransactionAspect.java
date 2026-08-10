package com.campanha.shared.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspecto que aplica {@code SET LOCAL app.current_partido_id} a cada método
 * {@code @Transactional} de camada application.service.
 *
 * SET LOCAL vive apenas até COMMIT/ROLLBACK (decisão D-05), então quando a
 * conexão volta ao pool o valor morre — sem risco de vazamento entre requests.
 */
@Aspect
@Component
@Order(Integer.MIN_VALUE + 20) // roda depois do TransactionInterceptor (que é MIN_VALUE)
@Slf4j
public class TenantAwareTransactionAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* com.campanha..application.service..*(..))")
    public Object applyTenantOnTransaction(ProceedingJoinPoint pjp) throws Throwable {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        Long tenantId = TenantContext.get();
        log.debug("TenantAware: método={}, txAtiva={}, tenant={}",
                pjp.getSignature().toShortString(), txActive, tenantId);
        if (!txActive) {
            return pjp.proceed();
        }
        String valor = tenantId == null ? "" : tenantId.toString();
        entityManager.createNativeQuery(
                "SELECT set_config('app.current_partido_id', :tenant, TRUE)")
                .setParameter("tenant", valor)
                .getSingleResult();
        return pjp.proceed();
    }
}
