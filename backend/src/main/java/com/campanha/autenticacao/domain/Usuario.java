package com.campanha.autenticacao.domain;

import java.time.Instant;

public record Usuario(
        Long id,
        Long partidoId,
        String nome,
        String email,
        String senhaHash,
        String telefone,
        Perfil perfil,
        boolean ativo,
        Instant criadoEm
) {
    public Usuario {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do usuário é obrigatório");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email do usuário é obrigatório");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("perfil do usuário é obrigatório");
        }
        if (perfil == Perfil.SUPER_ADMIN_PLATAFORMA && partidoId != null) {
            throw new IllegalArgumentException(
                    "SUPER_ADMIN_PLATAFORMA não pode estar vinculado a partido");
        }
        if (perfil != Perfil.SUPER_ADMIN_PLATAFORMA && partidoId == null) {
            throw new IllegalArgumentException(
                    "Perfil " + perfil + " requer partido_id");
        }
    }
}
