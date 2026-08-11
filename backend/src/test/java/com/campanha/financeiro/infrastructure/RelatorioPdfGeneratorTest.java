package com.campanha.financeiro.infrastructure;

import com.campanha.financeiro.application.port.in.FinanceiroUseCases.RelatorioFinanceiroJson;
import com.campanha.financeiro.application.port.in.FinanceiroUseCases.TotalPorCategoria;
import com.campanha.financeiro.domain.CategoriaDespesa;
import com.campanha.financeiro.infrastructure.adapter.in.web.RelatorioPdfGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioPdfGeneratorTest {

    @Test
    void geraPdfNaoVazioParaRelatorioValido() {
        RelatorioFinanceiroJson dados = new RelatorioFinanceiroJson(
                100L,
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("20000.00"),
                List.of(
                        new TotalPorCategoria(CategoriaDespesa.PESSOAL, new BigDecimal("15000.00")),
                        new TotalPorCategoria(CategoriaDespesa.MATERIAL_GRAFICO, new BigDecimal("10000.00")),
                        new TotalPorCategoria(CategoriaDespesa.TRANSPORTE, new BigDecimal("5000.00"))
                )
        );

        byte[] pdf = new RelatorioPdfGenerator().gerar(dados);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "PDF muito pequeno — possivelmente inválido: " + pdf.length + " bytes");
        // Header PDF: "%PDF-"
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void geraPdfComListaDeCategoriasVazia() {
        RelatorioFinanceiroJson dados = new RelatorioFinanceiroJson(
                100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()
        );
        byte[] pdf = new RelatorioPdfGenerator().gerar(dados);
        assertTrue(pdf.length > 500);
    }
}
