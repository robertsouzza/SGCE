package com.campanha.partido.domain;

public class MunicipioObrigatorioException extends IllegalArgumentException {
    public MunicipioObrigatorioException(Cargo cargo) {
        super("O cargo " + cargo + " exige o preenchimento do município.");
    }
}
