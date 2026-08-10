package com.campanha.partido.application.port.in;

import com.campanha.partido.domain.Partido;

public interface CadastrarPartidoUseCase {
    Partido executar(CadastrarPartidoCommand cmd);

    record CadastrarPartidoCommand(
            String nome,
            String sigla,
            int numeroPartido,
            String cnpj,
            String enderecoSede,
            String dadosBancariosContaPartidaria,
            String email,
            String telefone,
            String planoAssinatura
    ) {}
}
