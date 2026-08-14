package com.campanha.temporeal.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Ponte Redis Pub/Sub → STOMP: recebe mensagem do canal
 * "sgce:tempo-real:partido:{id}" e retransmite via WebSocket para
 * "/topic/tempo-real/{id}", escopado por partido.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventListener {

    private final SimpMessagingTemplate stomp;

    public void onMessage(String messageJson, String channel) {
        // Canal: sgce:tempo-real:partido:42 → destino STOMP: /topic/tempo-real/42
        String prefix = "sgce:tempo-real:partido:";
        if (channel == null || !channel.startsWith(prefix)) {
            return;
        }
        String partidoId = channel.substring(prefix.length());
        String destino = "/topic/tempo-real/" + partidoId;
        stomp.convertAndSend(destino, messageJson);
        log.debug("STOMP bridge: {} → {}", channel, destino);
    }
}
