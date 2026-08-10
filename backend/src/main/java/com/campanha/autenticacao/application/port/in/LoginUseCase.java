package com.campanha.autenticacao.application.port.in;

import com.campanha.autenticacao.domain.Usuario;

public interface LoginUseCase {
    Usuario autenticar(String email, String senhaPlana);
}
