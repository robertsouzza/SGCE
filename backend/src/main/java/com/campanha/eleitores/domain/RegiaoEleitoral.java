package com.campanha.eleitores.domain;

public record RegiaoEleitoral(
        Long id,
        NivelRegiao nivel,
        Long regiaoPaiId,
        String codigoIbge,
        String nomeRegiao
) {
    public RegiaoEleitoral {
        if (nivel == null) {
            throw new IllegalArgumentException("nivel é obrigatório");
        }
        if (nomeRegiao == null || nomeRegiao.isBlank()) {
            throw new IllegalArgumentException("nomeRegiao é obrigatório");
        }
    }
}
