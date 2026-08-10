package com.campanha.autenticacao.infrastructure.adapter.out.persistence;

import com.campanha.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.campanha.autenticacao.domain.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioJpaAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repo;

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return repo.findByEmail(email).map(UsuarioJpaAdapter::toDomain);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return repo.findById(id).map(UsuarioJpaAdapter::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioJpaEntity entity = UsuarioJpaEntity.builder()
                .id(usuario.id())
                .partidoId(usuario.partidoId())
                .nome(usuario.nome())
                .email(usuario.email())
                .senhaHash(usuario.senhaHash())
                .telefone(usuario.telefone())
                .perfil(usuario.perfil())
                .ativo(usuario.ativo())
                .criadoEm(usuario.criadoEm())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    static Usuario toDomain(UsuarioJpaEntity e) {
        return new Usuario(
                e.getId(),
                e.getPartidoId(),
                e.getNome(),
                e.getEmail(),
                e.getSenhaHash(),
                e.getTelefone(),
                e.getPerfil(),
                e.isAtivo(),
                e.getCriadoEm()
        );
    }
}
