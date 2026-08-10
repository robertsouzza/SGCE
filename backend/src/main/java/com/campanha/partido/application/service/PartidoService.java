package com.campanha.partido.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.partido.application.port.in.CadastrarPartidoUseCase;
import com.campanha.partido.application.port.in.ListarPartidosUseCase;
import com.campanha.partido.application.port.out.PartidoRepositoryPort;
import com.campanha.partido.domain.Partido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartidoService implements CadastrarPartidoUseCase, ListarPartidosUseCase {

    private final PartidoRepositoryPort repo;

    @Override
    @Transactional
    @Auditavel(acao = "cadastrar_partido", entidade = "Partido")
    public Partido executar(CadastrarPartidoCommand cmd) {
        if (repo.existsBySigla(cmd.sigla())) {
            throw new IllegalArgumentException("já existe partido com a sigla " + cmd.sigla());
        }
        if (repo.existsByCnpj(cmd.cnpj())) {
            throw new IllegalArgumentException("já existe partido com o CNPJ " + cmd.cnpj());
        }
        Partido novo = new Partido(
                null,
                cmd.nome(),
                cmd.sigla(),
                cmd.numeroPartido(),
                cmd.cnpj(),
                cmd.enderecoSede(),
                cmd.dadosBancariosContaPartidaria(),
                cmd.email(),
                cmd.telefone(),
                cmd.planoAssinatura() != null ? cmd.planoAssinatura() : "FREE",
                true,
                Instant.now()
        );
        return repo.save(novo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partido> executar() {
        return repo.findAll();
    }
}
