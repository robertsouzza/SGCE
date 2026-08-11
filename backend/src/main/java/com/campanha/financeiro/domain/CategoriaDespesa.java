package com.campanha.financeiro.domain;

public enum CategoriaDespesa {
    PESSOAL,
    ALIMENTACAO,
    TRANSPORTE,
    MATERIAL_GRAFICO,
    OUTROS;

    public boolean exigePagamentoEquipe() {
        return this == PESSOAL;
    }
}
