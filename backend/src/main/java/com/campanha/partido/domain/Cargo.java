package com.campanha.partido.domain;

public enum Cargo {
    PRESIDENTE,
    SENADOR,
    DEPUTADO_FEDERAL,
    DEPUTADO_ESTADUAL,
    PREFEITO,
    VEREADOR;

    /** PREFEITO e VEREADOR são cargos municipais → exigem município. */
    public boolean exigeMunicipio() {
        return this == PREFEITO || this == VEREADOR;
    }
}
