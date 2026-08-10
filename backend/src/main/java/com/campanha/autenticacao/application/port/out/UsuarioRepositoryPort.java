package com.campanha.autenticacao.application.port.out;

import com.campanha.autenticacao.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findById(Long id);
    Usuario save(Usuario usuario);
    boolean existsByEmail(String email);
}
