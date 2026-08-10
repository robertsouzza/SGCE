package com.campanha.partido.application.port.in;

import com.campanha.partido.domain.Candidato;

import java.util.List;

public interface ListarCandidatosUseCase {
    List<Candidato> executar();
}
