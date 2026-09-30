package com.campanha.partido.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.partido.application.port.in.CadastrarCandidatoUseCase;
import com.campanha.partido.application.port.in.ListarCandidatosUseCase;
import com.campanha.partido.application.port.out.CandidatoRepositoryPort;
import com.campanha.partido.domain.Candidato;
import com.campanha.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidatoService implements CadastrarCandidatoUseCase, ListarCandidatosUseCase {

    private final CandidatoRepositoryPort repo;

    @Override
    @Transactional
    @Auditavel(acao = "cadastrar_candidato", entidade = "Candidato")
    public Candidato executar(CadastrarCandidatoCommand cmd) {
        // Coerência multi-tenant: se o usuário é ADMIN (tem TenantContext),
        // não pode criar candidato em partido alheio; se é SUPER_ADMIN (contexto
        // vazio), pode escolher qualquer partido.
        Long tenantAtual = TenantContext.get();
        Long partidoAlvo = cmd.partidoId();
        if (tenantAtual != null && !tenantAtual.equals(partidoAlvo)) {
            throw new AccessDeniedException(
                    "usuário do partido " + tenantAtual + " não pode criar candidato em partido " + partidoAlvo);
        }
        if (repo.existsByTituloEleitorAndPartidoId(cmd.tituloEleitor(), partidoAlvo)) {
            throw new IllegalArgumentException(
                    "já existe candidato com título " + cmd.tituloEleitor() + " neste partido");
        }
        // Validações do domínio (cargo/uf/município) rodam no construtor do record:
        Candidato novo = new Candidato(
                null,
                partidoAlvo,
                cmd.usuarioId(),
                cmd.nomeCompleto(),
                cmd.tituloEleitor(),
                cmd.numeroCandidato(),
                cmd.cargo(),
                cmd.uf().toUpperCase(),
                cmd.municipio(),
                true,
                Instant.now()
        );
        return repo.save(novo);
    }

    @Transactional
    @Auditavel(acao = "alterar_ativo_candidato", entidade = "Candidato")
    public Candidato alterarAtivo(Long candidatoId, boolean ativo) {
        Candidato atual = repo.findById(candidatoId)
                .orElseThrow(() -> new IllegalArgumentException("candidato não encontrado: " + candidatoId));
        Long tenantAtual = TenantContext.get();
        if (tenantAtual != null && !tenantAtual.equals(atual.partidoId())) {
            throw new AccessDeniedException(
                    "usuário do partido " + tenantAtual + " não pode alterar candidato do partido " + atual.partidoId());
        }
        if (atual.ativo() == ativo) {
            return atual;
        }
        return repo.save(atual.comAtivo(ativo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Candidato> executar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Candidato> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional
    @Auditavel(acao = "atualizar_candidato", entidade = "Candidato")
    public Candidato atualizar(Long candidatoId, AtualizarCandidatoCommand cmd) {
        Candidato atual = repo.findById(candidatoId)
                .orElseThrow(() -> new IllegalArgumentException("candidato não encontrado: " + candidatoId));
        Long tenantAtual = TenantContext.get();
        if (tenantAtual != null && !tenantAtual.equals(atual.partidoId())) {
            throw new AccessDeniedException(
                    "usuário do partido " + tenantAtual + " não pode alterar candidato do partido " + atual.partidoId());
        }
        if (!atual.tituloEleitor().equals(cmd.tituloEleitor())
                && repo.existsByTituloEleitorAndPartidoId(cmd.tituloEleitor(), atual.partidoId())) {
            throw new IllegalArgumentException(
                    "já existe candidato com título " + cmd.tituloEleitor() + " neste partido");
        }
        Candidato atualizado = new Candidato(
                atual.id(),
                atual.partidoId(),
                atual.usuarioId(),
                cmd.nomeCompleto(),
                cmd.tituloEleitor(),
                cmd.numeroCandidato(),
                cmd.cargo(),
                cmd.uf().toUpperCase(),
                cmd.municipio(),
                atual.ativo(),
                atual.criadoEm()
        );
        return repo.save(atualizado);
    }

    public record AtualizarCandidatoCommand(
            String nomeCompleto,
            String tituloEleitor,
            int numeroCandidato,
            com.campanha.partido.domain.Cargo cargo,
            String uf,
            String municipio
    ) {}
}
