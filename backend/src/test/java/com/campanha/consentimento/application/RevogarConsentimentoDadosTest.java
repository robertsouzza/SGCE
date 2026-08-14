package com.campanha.consentimento.application;

import com.campanha.consentimento.application.port.out.AssinaturaStoragePort;
import com.campanha.consentimento.application.port.out.ConsentimentoRepositoryPort;
import com.campanha.consentimento.application.port.out.TermoRepositoryPort;
import com.campanha.consentimento.application.service.ConsentimentoService;
import com.campanha.consentimento.application.service.DeepLinkService;
import com.campanha.consentimento.domain.ConsentimentoLGPD;
import com.campanha.consentimento.domain.EstadoConsentimento;
import com.campanha.consentimento.domain.MetodoCaptura;
import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.eleitores.domain.Ponto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RevogarConsentimentoDadosTest {

    private ConsentimentoRepositoryPort consentimentoRepo;
    private EleitorRepositoryPort eleitorRepo;
    private ConsentimentoService service;

    @BeforeEach
    void setup() {
        consentimentoRepo = mock(ConsentimentoRepositoryPort.class);
        eleitorRepo = mock(EleitorRepositoryPort.class);
        service = new ConsentimentoService(
                mock(TermoRepositoryPort.class),
                consentimentoRepo,
                mock(AssinaturaStoragePort.class),
                eleitorRepo,
                mock(DeepLinkService.class));
    }

    @Test
    void revogarDadosAnonimizaEleitorMasPreservaConsentimentoComoProva() {
        Long consentimentoId = 10L;
        Long eleitorId = 100L;

        ConsentimentoLGPD original = new ConsentimentoLGPD(
                consentimentoId, 1L, eleitorId, 200L, 5L,
                MetodoCaptura.QRCODE_WHATSAPP, null, 42L,
                new Ponto(-46.63, -23.55), Instant.now(), Instant.now(),
                false, "COD001",
                EstadoConsentimento.conceder(),
                EstadoConsentimento.conceder(),
                Instant.now());

        Eleitor eleitorReal = new Eleitor(
                eleitorId, 1L, "João da Silva", "Rua X", new Ponto(-46.63, -23.55),
                "11999999999", "T123", null, "001", "0042", "obs",
                false, null, Instant.now(), Instant.now());

        when(consentimentoRepo.findById(consentimentoId)).thenReturn(Optional.of(original));
        when(consentimentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eleitorRepo.findById(eleitorId)).thenReturn(Optional.of(eleitorReal));
        when(eleitorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revogarConsentimentoDados(consentimentoId);

        // 1) ConsentimentoLGPD foi salvo com revogação — NÃO foi apagado
        ArgumentCaptor<ConsentimentoLGPD> cCaptor = ArgumentCaptor.forClass(ConsentimentoLGPD.class);
        verify(consentimentoRepo).save(cCaptor.capture());
        ConsentimentoLGPD salvo = cCaptor.getValue();
        assertFalse(salvo.tratamentoDadosVigente(), "consentimento_dados deveria estar revogado");
        assertTrue(salvo.consentimentoDados().revogado());
        assertTrue(salvo.marketingWhatsappVigente(), "marketing WhatsApp NÃO deveria ser tocado");
        assertNotNull(salvo.id(), "ConsentimentoLGPD original preservado (mesma linha)");

        // 2) Eleitor foi anonimizado — PII foi para NULL, id mantido
        ArgumentCaptor<Eleitor> eCaptor = ArgumentCaptor.forClass(Eleitor.class);
        verify(eleitorRepo).save(eCaptor.capture());
        Eleitor anonimizado = eCaptor.getValue();
        assertEquals(eleitorId, anonimizado.id(), "id do Eleitor preservado (chave estrangeira das Abordagens sobrevive)");
        assertTrue(anonimizado.anonimizado());
        assertNull(anonimizado.telefoneWhatsapp());
        assertNull(anonimizado.endereco());
        assertNull(anonimizado.geolocalizacao());
        assertNull(anonimizado.tituloEleitor());
        assertNotNull(anonimizado.tituloEleitorHash(), "hash preservado para dedupe futuro");
    }

    @Test
    void revogarWhatsappNaoAnonimizaEleitor() {
        Long consentimentoId = 11L;
        ConsentimentoLGPD original = new ConsentimentoLGPD(
                consentimentoId, 1L, 100L, null, 5L,
                MetodoCaptura.QRCODE_WHATSAPP, null, 42L,
                null, null, Instant.now(), false, null,
                EstadoConsentimento.conceder(), EstadoConsentimento.conceder(),
                Instant.now());
        when(consentimentoRepo.findById(consentimentoId)).thenReturn(Optional.of(original));
        when(consentimentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revogarConsentimentoWhatsApp(consentimentoId);

        verify(eleitorRepo, never()).findById(any());
        verify(eleitorRepo, never()).save(any());
    }
}
