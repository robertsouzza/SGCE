package com.campanha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
// TransactionInterceptor precisa ser outer (mais externo) para que a
// transação já esteja ativa quando TenantAwareTransactionAspect roda o
// SET LOCAL app.current_partido_id. Se ficar no default (LOWEST_PRECEDENCE),
// nosso aspect vira o outer, e o SET LOCAL nunca é aplicado — quebra RLS.
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
@EnableAsync
@EnableScheduling
public class SgceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SgceApplication.class, args);
    }
}
