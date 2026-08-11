package com.campanha.eleitores.application.service;

import com.campanha.auditoria.application.port.out.LogAuditoriaRepositoryPort;
import com.campanha.auditoria.domain.LogAuditoria;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.OperacaoSync;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.ResultadoLoteSync;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.ResultadoOpSync;
import com.campanha.eleitores.application.port.out.AbordagemRepositoryPort;
import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.application.port.out.SyncOpLogPort;
import com.campanha.eleitores.domain.Abordagem;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.eleitores.domain.IntencaoVoto;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.TipoAbordagem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coração da skill 04: processa lote de operações vindas do app offline
 * (D-04). Regras:
 *
 * - <b>Idempotência:</b> se o client_op_id já foi processado, retorna o mesmo
 *   server_id sem reprocessar.
 * - <b>Last-write-wins:</b> se veio update de Eleitor com timestamp_local mais
 *   recente que o existente, sobrescreve e loga dados_antes no LogAuditoria
 *   para recovery manual. Se o timestamp_local do incoming for MAIS ANTIGO
 *   que o do registro no servidor, ignora (server-side vence).
 * - <b>Clock skew:</b> se timestamp_local > 24h à frente do relógio do
 *   servidor, rejeita com CLOCK_SKEW — o app precisa mostrar mensagem
 *   pedindo que o usuário verifique o relógio do dispositivo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SincronizacaoService {

    static final Duration CLOCK_SKEW_TOLERANCIA = Duration.ofHours(24);

    private final EleitorRepositoryPort eleitorRepo;
    private final AbordagemRepositoryPort abordagemRepo;
    private final SyncOpLogPort syncLog;
    private final LogAuditoriaRepositoryPort auditoriaRepo;
    private final ObjectMapper objectMapper;

    public ResultadoLoteSync processarLote(List<OperacaoSync> operacoes, Long partidoId, Long membroId) {
        List<ResultadoOpSync> resultados = new ArrayList<>();
        Instant agora = Instant.now();

        for (OperacaoSync op : operacoes) {
            resultados.add(processarUma(op, partidoId, membroId, agora));
        }

        return new ResultadoLoteSync(resultados);
    }

    private ResultadoOpSync processarUma(OperacaoSync op, Long partidoId, Long membroId, Instant agora) {
        // 1) Idempotência: já vimos esse client_op_id?
        Optional<SyncOpLogPort.Registrado> jaProcessado = syncLog.find(op.clientOpId());
        if (jaProcessado.isPresent()) {
            SyncOpLogPort.Registrado r = jaProcessado.get();
            return new ResultadoOpSync(op.clientOpId(), "IDEMPOTENT_OK", r.serverEntityId(),
                    "já processada anteriormente");
        }

        // 2) Clock skew: dispositivo com relógio muito à frente?
        if (op.timestampLocal() != null
                && op.timestampLocal().isAfter(agora.plus(CLOCK_SKEW_TOLERANCIA))) {
            return new ResultadoOpSync(op.clientOpId(), "CLOCK_SKEW", null,
                    "timestamp_local do dispositivo está >24h no futuro. Verifique o relógio.");
        }

        // 3) Roteamento por entidade
        try {
            return switch (op.entidade()) {
                case "eleitor" -> processarEleitor(op, partidoId);
                case "abordagem" -> processarAbordagem(op, partidoId, membroId);
                default -> new ResultadoOpSync(op.clientOpId(), "VALIDATION_ERROR", null,
                        "entidade desconhecida: " + op.entidade());
            };
        } catch (Exception e) {
            log.warn("erro ao processar op {}: {}", op.clientOpId(), e.getMessage());
            return new ResultadoOpSync(op.clientOpId(), "VALIDATION_ERROR", null, e.getMessage());
        }
    }

    private ResultadoOpSync processarEleitor(OperacaoSync op, Long partidoId) {
        JsonNode p = objectMapper.valueToTree(op.payload());
        String titulo = text(p, "tituloEleitor");
        if (titulo == null || titulo.isBlank()) {
            return new ResultadoOpSync(op.clientOpId(), "VALIDATION_ERROR", null, "tituloEleitor é obrigatório");
        }

        Optional<Eleitor> existente = eleitorRepo.findByTituloEleitorAndPartidoId(titulo, partidoId);
        String status;
        Eleitor salvo;

        if (existente.isPresent()) {
            Eleitor atual = existente.get();
            // Last-write-wins: só sobrescreve se incoming é mais recente que o atualizado_em do servidor
            if (op.timestampLocal() == null || op.timestampLocal().isAfter(atual.atualizadoEm())) {
                // Registra dados_antes no LogAuditoria antes de sobrescrever
                auditoriaRepo.save(new LogAuditoria(
                        null, null, "sync_conflict_resolved", "Eleitor",
                        String.valueOf(atual.id()),
                        serialize(atual), p.toString(),
                        Instant.now(), null));
                salvo = eleitorRepo.save(construirEleitor(p, partidoId, atual, false));
                status = "CONFLICT_RESOLVED";
            } else {
                // Incoming é mais antigo — mantém o do servidor
                salvo = atual;
                status = "IGNORED_OLDER";
            }
        } else {
            salvo = eleitorRepo.save(construirEleitor(p, partidoId, null, true));
            status = "CREATED";
        }

        syncLog.save(op.clientOpId(), partidoId, "eleitor", salvo.id(), status);
        return new ResultadoOpSync(op.clientOpId(), status, salvo.id(), null);
    }

    private ResultadoOpSync processarAbordagem(OperacaoSync op, Long partidoId, Long membroId) {
        JsonNode p = objectMapper.valueToTree(op.payload());
        Long eleitorId = longVal(p, "eleitorId");
        if (eleitorId == null) {
            return new ResultadoOpSync(op.clientOpId(), "VALIDATION_ERROR", null, "eleitorId é obrigatório");
        }
        TipoAbordagem tipo = TipoAbordagem.valueOf(text(p, "tipoAbordagem"));
        Instant dataHora = p.hasNonNull("dataHora") ? Instant.parse(p.get("dataHora").asText()) : Instant.now();

        List<IntencaoVoto> intencoes = new ArrayList<>();
        if (p.has("intencoes") && p.get("intencoes").isArray()) {
            for (JsonNode i : p.get("intencoes")) {
                intencoes.add(new IntencaoVoto(null, partidoId, null,
                        i.get("candidatoId").asLong(),
                        com.campanha.eleitores.domain.Intencao.valueOf(i.get("intencao").asText())));
            }
        }

        Ponto geo = null;
        if (p.has("geolocalizacao") && p.get("geolocalizacao").isObject()) {
            geo = new Ponto(
                    p.get("geolocalizacao").get("longitude").asDouble(),
                    p.get("geolocalizacao").get("latitude").asDouble());
        }

        Abordagem a = new Abordagem(
                null, partidoId, eleitorId, membroId, longVal(p, "equipeId"),
                tipo, dataHora, geo, op.timestampLocal(), Instant.now(),
                true, intencoes, Instant.now());
        Abordagem salvo = abordagemRepo.save(a);
        syncLog.save(op.clientOpId(), partidoId, "abordagem", salvo.id(), "CREATED");
        return new ResultadoOpSync(op.clientOpId(), "CREATED", salvo.id(), null);
    }

    private Eleitor construirEleitor(JsonNode p, Long partidoId, Eleitor existente, boolean novo) {
        Ponto geo = null;
        if (p.has("geolocalizacao") && p.get("geolocalizacao").isObject()) {
            geo = new Ponto(
                    p.get("geolocalizacao").get("longitude").asDouble(),
                    p.get("geolocalizacao").get("latitude").asDouble());
        }
        return new Eleitor(
                novo ? null : existente.id(),
                partidoId,
                text(p, "nomeCompleto"),
                text(p, "endereco"),
                geo,
                text(p, "telefoneWhatsapp"),
                text(p, "tituloEleitor"),
                null,
                text(p, "zonaEleitoral"),
                text(p, "secaoEleitoral"),
                text(p, "observacoes"),
                false, null,
                novo ? Instant.now() : existente.criadoEm(),
                Instant.now()
        );
    }

    private String text(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asText() : null;
    }

    private Long longVal(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asLong() : null;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"erro\":\"" + e.getMessage() + "\"}";
        }
    }
}
