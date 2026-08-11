package com.campanha.eleitores.application;

import com.campanha.auditoria.application.port.out.LogAuditoriaRepositoryPort;
import com.campanha.auditoria.domain.LogAuditoria;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.OperacaoSync;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.ResultadoLoteSync;
import com.campanha.eleitores.application.port.in.EleitoresUseCases.ResultadoOpSync;
import com.campanha.eleitores.application.port.out.AbordagemRepositoryPort;
import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.application.port.out.SyncOpLogPort;
import com.campanha.eleitores.application.service.SincronizacaoService;
import com.campanha.eleitores.domain.Eleitor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SincronizacaoServiceTest {

    private EleitorRepositoryPort eleitorRepo;
    private AbordagemRepositoryPort abordagemRepo;
    private SyncOpLogPort syncLog;
    private LogAuditoriaRepositoryPort auditoria;
    private SincronizacaoService service;

    private static final Long PARTIDO = 1L;
    private static final Long MEMBRO = 42L;

    @BeforeEach
    void setup() {
        eleitorRepo = mock(EleitorRepositoryPort.class);
        abordagemRepo = mock(AbordagemRepositoryPort.class);
        syncLog = mock(SyncOpLogPort.class);
        auditoria = mock(LogAuditoriaRepositoryPort.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // habilita jackson-datatype-jsr310 para Instant/LocalDate
        service = new SincronizacaoService(eleitorRepo, abordagemRepo, syncLog, auditoria, mapper);
    }

    @Test
    void idempotenciaRetornaMesmoServerIdSemReprocessar() {
        UUID opId = UUID.randomUUID();
        when(syncLog.find(opId)).thenReturn(Optional.of(
                new SyncOpLogPort.Registrado(opId, "eleitor", 999L, "CREATED")));

        OperacaoSync op = new OperacaoSync(opId, "eleitor", "CREATE",
                Map.of("tituloEleitor", "X"), Instant.now());

        ResultadoLoteSync r = service.processarLote(List.of(op), PARTIDO, MEMBRO);

        assertEquals(1, r.resultados().size());
        assertEquals("IDEMPOTENT_OK", r.resultados().get(0).status());
        assertEquals(999L, r.resultados().get(0).serverId());
        verify(eleitorRepo, never()).save(any());
        verify(syncLog, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void clockSkewRejeitaTimestampMaisDe24hNoFuturo() {
        UUID opId = UUID.randomUUID();
        when(syncLog.find(opId)).thenReturn(Optional.empty());

        Instant futuroLonge = Instant.now().plus(Duration.ofHours(48));
        OperacaoSync op = new OperacaoSync(opId, "eleitor", "CREATE",
                Map.of("tituloEleitor", "X"), futuroLonge);

        ResultadoLoteSync r = service.processarLote(List.of(op), PARTIDO, MEMBRO);
        assertEquals("CLOCK_SKEW", r.resultados().get(0).status());
        verify(eleitorRepo, never()).save(any());
    }

    @Test
    void novoEleitorEhCriadoQuandoNaoExiste() {
        UUID opId = UUID.randomUUID();
        when(syncLog.find(opId)).thenReturn(Optional.empty());
        when(eleitorRepo.findByTituloEleitorAndPartidoId("T1", PARTIDO)).thenReturn(Optional.empty());
        when(eleitorRepo.save(any())).thenAnswer(inv -> {
            Eleitor e = inv.getArgument(0);
            return new Eleitor(555L, e.partidoId(), e.nomeCompleto(), e.endereco(),
                    e.geolocalizacao(), e.telefoneWhatsapp(), e.tituloEleitor(),
                    e.tituloEleitorHash(), e.zonaEleitoral(), e.secaoEleitoral(),
                    e.observacoes(), e.anonimizado(), e.anonimizadoEm(),
                    e.criadoEm(), e.atualizadoEm());
        });

        OperacaoSync op = new OperacaoSync(opId, "eleitor", "CREATE",
                Map.of("nomeCompleto", "João", "tituloEleitor", "T1"), Instant.now());

        ResultadoOpSync r = service.processarLote(List.of(op), PARTIDO, MEMBRO).resultados().get(0);
        assertEquals("CREATED", r.status());
        assertEquals(555L, r.serverId());
        verify(syncLog).save(eq(opId), eq(PARTIDO), eq("eleitor"), eq(555L), eq("CREATED"));
    }

    @Test
    void updateComTimestampMaisRecenteResolveConflitoEAuditaAntes() {
        UUID opId = UUID.randomUUID();
        Instant antigo = Instant.now().minusSeconds(3600);
        Instant novo = Instant.now();

        Eleitor existente = new Eleitor(100L, PARTIDO, "João Antigo", "End Antigo",
                null, "111", "T1", null, null, null, null, false, null, antigo, antigo);

        when(syncLog.find(opId)).thenReturn(Optional.empty());
        when(eleitorRepo.findByTituloEleitorAndPartidoId("T1", PARTIDO)).thenReturn(Optional.of(existente));
        when(eleitorRepo.save(any())).thenAnswer(inv -> {
            Eleitor e = inv.getArgument(0);
            return new Eleitor(100L, e.partidoId(), e.nomeCompleto(), e.endereco(),
                    e.geolocalizacao(), e.telefoneWhatsapp(), e.tituloEleitor(),
                    e.tituloEleitorHash(), e.zonaEleitoral(), e.secaoEleitoral(),
                    e.observacoes(), e.anonimizado(), e.anonimizadoEm(),
                    e.criadoEm(), e.atualizadoEm());
        });

        OperacaoSync op = new OperacaoSync(opId, "eleitor", "UPDATE",
                Map.of("nomeCompleto", "João Novo", "tituloEleitor", "T1"), novo);

        ResultadoOpSync r = service.processarLote(List.of(op), PARTIDO, MEMBRO).resultados().get(0);
        assertEquals("CONFLICT_RESOLVED", r.status());
        assertEquals(100L, r.serverId());
        verify(auditoria).save(argThat((LogAuditoria l) ->
                "sync_conflict_resolved".equals(l.acao()) &&
                "Eleitor".equals(l.entidade()) &&
                l.dadosAntes().contains("João Antigo")));
    }

    @Test
    void updateComTimestampMaisAntigoEIgnorado() {
        UUID opId = UUID.randomUUID();
        Instant maisNovo = Instant.now();
        Instant maisAntigo = maisNovo.minusSeconds(7200);

        Eleitor existente = new Eleitor(100L, PARTIDO, "Recente", null, null,
                null, "T1", null, null, null, null, false, null, maisNovo, maisNovo);

        when(syncLog.find(opId)).thenReturn(Optional.empty());
        when(eleitorRepo.findByTituloEleitorAndPartidoId("T1", PARTIDO)).thenReturn(Optional.of(existente));

        OperacaoSync op = new OperacaoSync(opId, "eleitor", "UPDATE",
                Map.of("nomeCompleto", "Muito Antigo", "tituloEleitor", "T1"), maisAntigo);

        ResultadoOpSync r = service.processarLote(List.of(op), PARTIDO, MEMBRO).resultados().get(0);
        assertEquals("IGNORED_OLDER", r.status());
        assertEquals(100L, r.serverId());
        verify(eleitorRepo, never()).save(any());
        verify(auditoria, never()).save(any());
    }
}
