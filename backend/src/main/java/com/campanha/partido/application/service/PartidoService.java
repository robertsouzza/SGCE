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
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public Optional<Partido> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional
    @Auditavel(acao = "alterar_ativo_partido", entidade = "Partido")
    public Partido alterarAtivo(Long partidoId, boolean ativo) {
        Partido atual = repo.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("partido não encontrado: " + partidoId));
        if (atual.ativo() == ativo) {
            return atual;
        }
        Partido atualizado = new Partido(
                atual.id(), atual.nome(), atual.sigla(), atual.numeroPartido(),
                atual.cnpj(), atual.enderecoSede(), atual.dadosBancariosContaPartidaria(),
                atual.email(), atual.telefone(), atual.planoAssinatura(),
                ativo, atual.criadoEm()
        );
        return repo.save(atualizado);
    }

    @Transactional
    @Auditavel(acao = "atualizar_partido", entidade = "Partido")
    public Partido atualizar(Long partidoId, AtualizarPartidoCommand cmd) {
        Partido atual = repo.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("partido não encontrado: " + partidoId));
        // Sigla e CNPJ são identificadores — se mudaram, checar unicidade.
        if (!atual.sigla().equalsIgnoreCase(cmd.sigla()) && repo.existsBySigla(cmd.sigla())) {
            throw new IllegalArgumentException("já existe partido com a sigla " + cmd.sigla());
        }
        if (!atual.cnpj().equals(cmd.cnpj()) && repo.existsByCnpj(cmd.cnpj())) {
            throw new IllegalArgumentException("já existe partido com o CNPJ " + cmd.cnpj());
        }
        Partido atualizado = new Partido(
                atual.id(), cmd.nome(), cmd.sigla(), cmd.numeroPartido(), cmd.cnpj(),
                cmd.enderecoSede(), cmd.dadosBancariosContaPartidaria(),
                cmd.email(), cmd.telefone(),
                cmd.planoAssinatura() != null ? cmd.planoAssinatura() : atual.planoAssinatura(),
                atual.ativo(), atual.criadoEm()
        );
        return repo.save(atualizado);
    }

    public record AtualizarPartidoCommand(
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
