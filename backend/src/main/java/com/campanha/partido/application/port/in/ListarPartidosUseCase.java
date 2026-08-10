package com.campanha.partido.application.port.in;

import com.campanha.partido.domain.Partido;

import java.util.List;

public interface ListarPartidosUseCase {
    List<Partido> executar();
}
