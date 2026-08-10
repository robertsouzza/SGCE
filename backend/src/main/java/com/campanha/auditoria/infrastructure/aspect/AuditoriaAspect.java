package com.campanha.auditoria.infrastructure.aspect;

import com.campanha.auditoria.application.port.out.LogAuditoriaRepositoryPort;
import com.campanha.auditoria.domain.Auditavel;
import com.campanha.auditoria.domain.LogAuditoria;
import com.campanha.autenticacao.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditoriaAspect {

    private final LogAuditoriaRepositoryPort repo;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditavel)")
    public Object auditar(ProceedingJoinPoint pjp, Auditavel auditavel) throws Throwable {
        String dadosAntes = serialize(pjp.getArgs().length > 0 ? pjp.getArgs()[0] : null);
        Object result = pjp.proceed();
        String dadosDepois = serialize(result);

        try {
            LogAuditoria entry = new LogAuditoria(
                    null,
                    currentUsuarioId(),
                    auditavel.acao(),
                    auditavel.entidade(),
                    extractId(result),
                    dadosAntes,
                    dadosDepois,
                    Instant.now(),
                    currentIp()
            );
            repo.save(entry);
        } catch (Exception e) {
            // Log de auditoria não pode derrubar a operação de negócio.
            log.error("Falha ao gravar log de auditoria (operação foi executada com sucesso)", e);
        }
        return result;
    }

    private String serialize(Object o) {
        if (o == null) return null;
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{\"__erro_serializacao__\":\"" + e.getMessage() + "\"}";
        }
    }

    private String extractId(Object result) {
        if (result == null) return null;
        try {
            var idMethod = result.getClass().getMethod("id");
            Object id = idMethod.invoke(result);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private Long currentUsuarioId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser au) {
            return au.usuarioId();
        }
        return null;
    }

    private String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
        }
        return null;
    }
}
