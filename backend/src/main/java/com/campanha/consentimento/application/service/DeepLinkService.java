package com.campanha.consentimento.application.service;

import com.campanha.consentimento.application.port.in.ConsentimentoUseCases.DeepLinkOptInResult;
import com.campanha.consentimento.application.port.out.ConsentimentoRepositoryPort;
import com.campanha.consentimento.application.port.out.WhatsAppOptInPort;
import com.campanha.partido.application.port.out.CandidatoRepositoryPort;
import com.campanha.partido.application.port.out.PartidoRepositoryPort;
import com.campanha.partido.domain.Candidato;
import com.campanha.partido.domain.Partido;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Gera o deep-link wa.me + QR code para o eleitor apontar o próprio celular
 * (D-01). Fluxo: eleitor abre WhatsApp com mensagem pré-preenchida →
 * aperta enviar → mensagem chega ao número da campanha do partido →
 * (integração real com WhatsApp fica fora do MVP; skill separada).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeepLinkService {

    private final PartidoRepositoryPort partidoRepo;
    private final CandidatoRepositoryPort candidatoRepo;
    private final ConsentimentoRepositoryPort consentimentoRepo;
    private final WhatsAppOptInPort whatsAppOptIn;

    public DeepLinkOptInResult gerar(Long abordagemId, Long candidatoId, Long partidoId) {
        Candidato cand = candidatoRepo.findById(candidatoId)
                .orElseThrow(() -> new IllegalArgumentException("candidato não encontrado"));
        Partido partido = partidoRepo.findById(partidoId)
                .orElseThrow(() -> new IllegalStateException("partido do tenant não encontrado"));

        String telefone = partido.telefone();
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalStateException(
                    "partido não tem telefone cadastrado (obrigatório para deep-link wa.me)");
        }
        String numeroE164 = onlyDigits(telefone);
        String cod = codCurto(abordagemId, candidatoId);

        String mensagem = "Autorizo receber conteúdo do candidato " + cand.nomeCompleto()
                + " (cod:" + cod + ")";
        String url = "https://wa.me/" + numeroE164
                + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);

        String qrDataUri = gerarQrCodePngDataUri(url);

        // Loga via stub — a integração real (skill separada) usaria este cod
        // depois para amarrar a mensagem recebida ao ConsentimentoLGPD.
        whatsAppOptIn.confirmarOptIn(numeroE164, cand.nomeCompleto(), cod);

        return new DeepLinkOptInResult(url, qrDataUri, cod);
    }

    private String gerarQrCodePngDataUri(String url) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 256, 256);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("falha ao gerar QR code: " + e.getMessage(), e);
        }
    }

    private String codCurto(Long abordagemId, Long candidatoId) {
        // Determinístico o suficiente para amarrar a mensagem recebida:
        long seed = abordagemId * 31L + candidatoId + System.currentTimeMillis();
        return Long.toString(Math.abs(seed), 36).toUpperCase().substring(0, Math.min(8, Long.toString(Math.abs(seed), 36).length()));
    }

    private String onlyDigits(String s) {
        return s.replaceAll("\\D", "");
    }
}
