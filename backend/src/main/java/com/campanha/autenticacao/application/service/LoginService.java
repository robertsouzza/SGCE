package com.campanha.autenticacao.application.service;

import com.campanha.autenticacao.application.port.in.LoginUseCase;
import com.campanha.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.campanha.autenticacao.domain.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Usuario autenticar(String email, String senhaPlana) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas."));

        if (!usuario.ativo()) {
            throw new BadCredentialsException("Usuário inativo.");
        }
        if (!passwordEncoder.matches(senhaPlana, usuario.senhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        return usuario;
    }
}
