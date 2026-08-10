package com.campanha.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sgceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGCE — Sistema de Gestão de Campanha Eleitoral 2026")
                        .description("API do backend do SGCE. Autenticação por cookie httpOnly + CSRF token.")
                        .version("0.1.0")
                        .license(new License().name("TBD")));
    }
}
