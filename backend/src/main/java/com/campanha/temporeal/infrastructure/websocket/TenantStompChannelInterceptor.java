package com.campanha.temporeal.infrastructure.websocket;

import com.campanha.autenticacao.domain.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Escopo de segurança do STOMP (RF-19): cliente autenticado como partido A
 * NÃO pode subscribe /topic/tempo-real/{partido B}.
 */
@Component
@Slf4j
public class TenantStompChannelInterceptor implements ChannelInterceptor {

    private static final String DESTINO_TEMPO_REAL_PREFIX = "/topic/tempo-real/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(h.getCommand())) {
            String destino = h.getDestination();
            if (destino != null && destino.startsWith(DESTINO_TEMPO_REAL_PREFIX)) {
                String partidoIdStr = destino.substring(DESTINO_TEMPO_REAL_PREFIX.length());
                Long partidoIdCanal;
                try {
                    partidoIdCanal = Long.valueOf(partidoIdStr);
                } catch (NumberFormatException e) {
                    throw new AccessDeniedException("destino inválido: " + destino);
                }
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
                    throw new AccessDeniedException("STOMP SUBSCRIBE requer autenticação");
                }
                if (au.partidoId() == null || !au.partidoId().equals(partidoIdCanal)) {
                    log.warn("bloqueado STOMP cross-tenant: usuario={} tentou {}, tenant dele={}",
                            au.usuarioId(), destino, au.partidoId());
                    throw new AccessDeniedException("cross-tenant STOMP subscribe negado");
                }
            }
        }
        return message;
    }
}
