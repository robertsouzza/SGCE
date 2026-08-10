package com.campanha.multitenancy;

import com.campanha.partido.application.port.in.CadastrarCandidatoUseCase;
import com.campanha.partido.application.port.in.CadastrarPartidoUseCase;
import com.campanha.partido.application.port.in.ListarCandidatosUseCase;
import com.campanha.partido.application.port.in.ListarPartidosUseCase;
import com.campanha.partido.domain.Candidato;
import com.campanha.partido.domain.Cargo;
import com.campanha.partido.domain.Partido;
import com.campanha.shared.multitenancy.TenantContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova, na prática, o isolamento multi-tenant (RNF-18): dois partidos no
 * mesmo banco — uma sessão como ADMIN do partido A NÃO enxerga dado do B.
 *
 * <p>Roda contra o Postgres real do docker-compose (`docker compose up postgres`
 * antes). Não usa Testcontainers porque, no ambiente WSL2 + Docker Desktop
 * atual, o DockerClientProviderStrategy falha ao detectar o daemon (bug
 * conhecido). O teste ainda prova o comportamento — a RLS é validada em
 * Postgres real com PostGIS real, exatamente como em prod.
 *
 * <p>Se este teste falhar, NÃO desabilite — investigue por que a RLS não
 * está sendo aplicada (candidato mais comum: método fora de {@code @Transactional},
 * então o SET LOCAL não roda; ou {@code FORCE ROW LEVEL SECURITY} esquecido
 * na migration).
 */
@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:postgresql://localhost:5433/sgce",
        "spring.datasource.username=sgce_app",
        "spring.datasource.password=sgce_app_dev",
        "spring.flyway.user=sgce",
        "spring.flyway.password=sgce_dev"
})
class MultiTenantIsolationIntegrationTest {

    @Autowired CadastrarPartidoUseCase cadastrarPartido;
    @Autowired ListarPartidosUseCase listarPartidos;
    @Autowired CadastrarCandidatoUseCase cadastrarCandidato;
    @Autowired ListarCandidatosUseCase listarCandidatos;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void limparBase() {
        TenantContext.clear();
        // TRUNCATE ignora RLS (é DDL, não DML) — resolve o problema de que
        // SUPER_ADMIN sem tenant não enxerga (e portanto não deleta) linhas
        // de partidos específicos. CASCADE cobre as FKs.
        tx.executeWithoutResult(status ->
                em.createNativeQuery(
                        "TRUNCATE partidos, candidatos, equipes, membros_equipe, equipe_candidato CASCADE")
                        .executeUpdate());
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    void adminDoPartidoANaoVeCandidatoDoPartidoB() {
        TenantContext.clear();
        Partido partidoA = cadastrarPartido.executar(new CadastrarPartidoUseCase.CadastrarPartidoCommand(
                "Partido A", "PAA", 11, "11111111000111", null, null, null, null, "FREE"));
        Partido partidoB = cadastrarPartido.executar(new CadastrarPartidoUseCase.CadastrarPartidoCommand(
                "Partido B", "PBB", 22, "22222222000122", null, null, null, null, "FREE"));

        // Candidato individual só pode ser criado por ADMIN do partido (RLS
        // proíbe SUPER_ADMIN de inserir em partido específico sem break-glass).
        TenantContext.set(partidoA.id());
        cadastrarCandidato.executar(new CadastrarCandidatoUseCase.CadastrarCandidatoCommand(
                partidoA.id(), null, "Candidato do A", "AAA111", 11, Cargo.SENADOR, "SP", null));
        TenantContext.set(partidoB.id());
        cadastrarCandidato.executar(new CadastrarCandidatoUseCase.CadastrarCandidatoCommand(
                partidoB.id(), null, "Candidato do B", "BBB222", 22, Cargo.SENADOR, "SP", null));

        TenantContext.clear();
        List<Candidato> todosSuperAdmin = listarCandidatos.executar();
        // Regra do modelo: SUPER_ADMIN sem sessão de suporte NÃO vê candidato
        // individual (conteúdo sensível) — só métricas agregadas. Acesso a
        // dados de um partido requer break-glass (skill 06).
        assertEquals(0, todosSuperAdmin.size(),
                "SUPER_ADMIN sem sessão de suporte NÃO deveria ver candidatos individuais");

        TenantContext.set(partidoA.id());
        List<Candidato> visaoA = listarCandidatos.executar();
        assertEquals(1, visaoA.size(), "ADMIN do partido A deveria ver apenas 1 candidato (o do A)");
        assertEquals("Candidato do A", visaoA.get(0).nomeCompleto());
        assertEquals(partidoA.id(), visaoA.get(0).partidoId());

        TenantContext.set(partidoB.id());
        List<Candidato> visaoB = listarCandidatos.executar();
        assertEquals(1, visaoB.size(), "ADMIN do partido B deveria ver apenas 1 candidato (o do B)");
        assertEquals("Candidato do B", visaoB.get(0).nomeCompleto());
    }

    @Test
    void adminDoPartidoAVeApenasSeuProprioPartidoNaListaDePartidos() {
        TenantContext.clear();
        Partido a = cadastrarPartido.executar(new CadastrarPartidoUseCase.CadastrarPartidoCommand(
                "Partido C", "PCC", 33, "33333333000133", null, null, null, null, "FREE"));
        Partido b = cadastrarPartido.executar(new CadastrarPartidoUseCase.CadastrarPartidoCommand(
                "Partido D", "PDD", 44, "44444444000144", null, null, null, null, "FREE"));

        TenantContext.set(a.id());
        List<Partido> visaoA = listarPartidos.executar();
        assertEquals(1, visaoA.size(), "ADMIN do partido A vê só seu partido");
        assertEquals(a.id(), visaoA.get(0).id());

        TenantContext.set(b.id());
        List<Partido> visaoB = listarPartidos.executar();
        assertEquals(1, visaoB.size(), "ADMIN do partido B vê só seu partido");
        assertEquals(b.id(), visaoB.get(0).id());
    }
}
