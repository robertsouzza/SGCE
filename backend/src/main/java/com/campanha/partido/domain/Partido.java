package com.campanha.partido.domain;

import java.time.Instant;

public record Partido(
        Long id,
        String nome,
        String sigla,
        int numeroPartido,
        String cnpj,
        String enderecoSede,
        String dadosBancariosContaPartidaria,
        String email,
        String telefone,
        String planoAssinatura,
        boolean ativo,
        Instant criadoEm
) {
    public Partido {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do partido é obrigatório");
        }
        if (sigla == null || sigla.isBlank()) {
            throw new IllegalArgumentException("sigla do partido é obrigatória");
        }
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("CNPJ do partido é obrigatório");
        }
        if (numeroPartido < 10 || numeroPartido > 99) {
            throw new IllegalArgumentException("número do partido deve ter 2 dígitos (10–99)");
        }
    }
}
