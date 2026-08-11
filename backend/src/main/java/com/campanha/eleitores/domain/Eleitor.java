package com.campanha.eleitores.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public record Eleitor(
        Long id,
        Long partidoId,
        String nomeCompleto,
        String endereco,
        Ponto geolocalizacao,
        String telefoneWhatsapp,
        String tituloEleitor,
        String tituloEleitorHash,
        String zonaEleitoral,
        String secaoEleitoral,
        String observacoes,
        boolean anonimizado,
        Instant anonimizadoEm,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public Eleitor {
        if (partidoId == null) {
            throw new IllegalArgumentException("eleitor precisa de partido");
        }
        if (!anonimizado) {
            if (nomeCompleto == null || nomeCompleto.isBlank()) {
                throw new IllegalArgumentException("nomeCompleto é obrigatório para eleitor não anonimizado");
            }
            if (tituloEleitor == null || tituloEleitor.isBlank()) {
                throw new IllegalArgumentException("tituloEleitor é obrigatório para eleitor não anonimizado");
            }
        }
    }

    /**
     * Retorna uma cópia com PII apagada — usado pelo caso de uso da skill 05
     * (D-02) ao revogar consentimento_dados. Mantém id, partido_id e um hash
     * do título para dedupe de re-cadastro futuro. Abordagens/intenções são
     * preservadas para agregados por região (sem PII vinculável).
     */
    public Eleitor anonimizar() {
        return new Eleitor(
                id,
                partidoId,
                "Eleitor anonimizado #" + id,
                null,
                null,
                null,
                null,
                sha256(tituloEleitor),
                null,
                null,
                null,
                true,
                Instant.now(),
                criadoEm,
                Instant.now()
        );
    }

    private static String sha256(String v) {
        if (v == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(v.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
