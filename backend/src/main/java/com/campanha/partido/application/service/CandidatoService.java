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
                Instant.now()
        );
        return repo.save(novo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Candidato> executar() {
        return repo.findAll();
    }
}
