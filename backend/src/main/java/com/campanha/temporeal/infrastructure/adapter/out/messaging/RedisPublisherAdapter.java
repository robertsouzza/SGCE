package com.campanha.temporeal.infrastructure.adapter.out.messaging;

import com.campanha.temporeal.application.port.out.LocalizacaoPublisherPort;
import com.campanha.temporeal.domain.LocalizacaoEquipe;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPublisherAdapter implements LocalizacaoPublisherPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    @Override
    public void publicarHeartbeat(LocalizacaoEquipe l) {
        Map<String, Object> payload = Map.of(
                "tipo", "heartbeat_membro",
                "membroId", l.membroId(),
                "partidoId", l.partidoId(),
                "geolocalizacao", Map.of(
                        "longitude", l.ponto().longitude(),
                        "latitude", l.ponto().latitude()),
                "statusConexao", l.statusConexao().name(),
                "timestamp", l.timestamp().toString()
        );
        publicarNoCanal(l.partidoId(), payload);
    }

    @Override
    public void publicarEvento(Long partidoId, String tipo, Object payload) {
        Map<String, Object> envelope = Map.of("tipo", tipo, "partidoId", partidoId, "payload", payload);
        publicarNoCanal(partidoId, envelope);
    }

    private void publicarNoCanal(Long partidoId, Object envelope) {
        String canal = "sgce:tempo-real:partido:" + partidoId;
        try {
            redis.convertAndSend(canal, mapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.error("falha ao publicar em {}: {}", canal, e.getMessage(), e);
        }
    }
}
