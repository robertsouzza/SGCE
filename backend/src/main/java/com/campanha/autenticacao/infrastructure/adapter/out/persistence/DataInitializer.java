package com.campanha.autenticacao.infrastructure.adapter.out.persistence;

import com.campanha.autenticacao.domain.Perfil;
import com.campanha.autenticacao.domain.Usuario;
import com.campanha.autenticacao.application.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Cria o usuário SUPER_ADMIN_PLATAFORMA sintético em dev/docker, caso não
 * exista. Senha fixa "changeme-in-prod" — comentário auto-explicativo.
 * NÃO roda em profile prod (skill 10 tratará bootstrap seguro em prod).
 */
@Component
@Profile({"dev", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String SUPER_ADMIN_EMAIL = "superadmin@sgce.local";
    private static final String SUPER_ADMIN_SENHA = "changeme-in-prod";

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(SUPER_ADMIN_EMAIL)) {
            log.info("SUPER_ADMIN sintético já existe — DataInitializer não fez nada.");
            return;
        }
        Usuario admin = new Usuario(
                null,
                null,
                "Super Administrador da Plataforma (sintético dev)",
                SUPER_ADMIN_EMAIL,
                passwordEncoder.encode(SUPER_ADMIN_SENHA),
                null,
                Perfil.SUPER_ADMIN_PLATAFORMA,
                true,
                Instant.now()
        );
        usuarioRepository.save(admin);
        log.warn("SUPER_ADMIN sintético criado: {} / {} — NUNCA usar em prod.",
                SUPER_ADMIN_EMAIL, SUPER_ADMIN_SENHA);
    }
}
