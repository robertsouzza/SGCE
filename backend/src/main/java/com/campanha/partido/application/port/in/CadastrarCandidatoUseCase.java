package com.campanha.partido.application.port.in;

import com.campanha.partido.domain.Candidato;
import com.campanha.partido.domain.Cargo;

public interface CadastrarCandidatoUseCase {
    Candidato executar(CadastrarCandidatoCommand cmd);

    record CadastrarCandidatoCommand(
            Long partidoId,
            Long usuarioId,
            String nomeCompleto,
            String tituloEleitor,
            int numeroCandidato,
            Cargo cargo,
            String uf,
            String municipio
    ) {}
}
