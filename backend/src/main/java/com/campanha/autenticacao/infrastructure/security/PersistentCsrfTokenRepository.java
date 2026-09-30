package com.campanha.autenticacao.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Envelopa {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository}
 * para NÃO invalidar o cookie XSRF-TOKEN após um POST/PUT/DELETE bem-sucedido.
 *
 * Motivação: Spring Security 6.1+ chama {@code saveToken(null, ...)} depois que o
 * {@code CsrfFilter} consome o token, o que emite um {@code Set-Cookie:
 * XSRF-TOKEN=; Max-Age=0} — proteção anti-replay pensada para páginas
 * server-side rendered. Em uma SPA, isso deixa o cliente sem token para a
 * próxima requisição mutante e ele leva 401/403 até a página ser recarregada.
 * Ignorar o saveToken(null) preserva o token entre requisições sem
 * comprometer o modelo double-submit-cookie (o cliente ainda precisa enviar
 * o header X-XSRF-TOKEN com o valor do cookie, que um origem externa não
 * consegue ler por Same-Origin Policy).
 */
public class PersistentCsrfTokenRepository implements CsrfTokenRepository {

    private final CsrfTokenRepository delegate;

    public PersistentCsrfTokenRepository(CsrfTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            // Ignora a tentativa do CsrfFilter de invalidar o token após uso.
            return;
        }
        delegate.saveToken(token, request, response);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
