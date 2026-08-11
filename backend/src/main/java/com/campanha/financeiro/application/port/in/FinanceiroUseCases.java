package com.campanha.financeiro.application.port.in;

import com.campanha.financeiro.domain.CategoriaDespesa;
import com.campanha.financeiro.domain.Despesa;
import com.campanha.financeiro.domain.PagamentoEquipe;
import com.campanha.financeiro.domain.RecursoFundoEleitoral;
import com.campanha.financeiro.domain.TipoPagamento;
import com.campanha.financeiro.domain.TipoRecurso;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Facade dos use cases financeiros. Mantém coeso o contrato in do módulo
 * (secretário lança, gerente aprova).
 */
public interface FinanceiroUseCases {

    RecursoFundoEleitoral registrarRecurso(RegistrarRecursoCommand cmd);

    Despesa lancarDespesa(LancarDespesaCommand cmd);

    Despesa aprovarDespesa(Long despesaId, Long aprovadorId);

    Despesa rejeitarDespesa(Long despesaId, Long aprovadorId, String motivo);

    PagamentoEquipe registrarPagamentoEquipe(RegistrarPagamentoEquipeCommand cmd);

    /** Faz upload e retorna a Despesa atualizada com a chave do comprovante. */
    Despesa anexarComprovanteDespesa(Long despesaId, InputStream content, long contentLength, String contentType, String nomeArquivo);

    String presignedUrlComprovanteDespesa(Long despesaId);

    List<Despesa> listarDespesas();
    List<RecursoFundoEleitoral> listarRecursos();

    RelatorioFinanceiroJson gerarRelatorioJson(Long candidatoId);

    record RegistrarRecursoCommand(
            Long candidatoId,
            TipoRecurso tipoRecurso,
            BigDecimal valor,
            LocalDate dataRepasse,
            String origem,
            String numeroDocumento
    ) {}

    record LancarDespesaCommand(
            Long candidatoId,
            CategoriaDespesa categoria,
            String subcategoriaTse,
            BigDecimal valor,
            LocalDate data,
            String descricao,
            Long lancadoPor
    ) {}

    record RegistrarPagamentoEquipeCommand(
            Long despesaId,
            Long membroId,
            TipoPagamento tipoPagamento,
            int quantidade,
            BigDecimal valorUnitario,
            String periodoReferencia
    ) {}

    record RelatorioFinanceiroJson(
            Long candidatoId,
            BigDecimal totalRecursos,
            BigDecimal totalDespesasAprovadas,
            BigDecimal saldoAtual,
            List<TotalPorCategoria> despesasPorCategoria
    ) {}

    record TotalPorCategoria(CategoriaDespesa categoria, BigDecimal total) {}
}
