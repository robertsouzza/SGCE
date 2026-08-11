package com.campanha.financeiro.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.financeiro.application.port.in.FinanceiroUseCases;
import com.campanha.financeiro.application.port.out.ComprovanteStoragePort;
import com.campanha.financeiro.application.port.out.DespesaRepositoryPort;
import com.campanha.financeiro.application.port.out.PagamentoEquipeRepositoryPort;
import com.campanha.financeiro.application.port.out.RecursoRepositoryPort;
import com.campanha.financeiro.domain.Despesa;
import com.campanha.financeiro.domain.PagamentoEquipe;
import com.campanha.financeiro.domain.RecursoFundoEleitoral;
import com.campanha.financeiro.domain.StatusDespesa;
import com.campanha.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinanceiroService implements FinanceiroUseCases {

    private final RecursoRepositoryPort recursoRepo;
    private final DespesaRepositoryPort despesaRepo;
    private final PagamentoEquipeRepositoryPort pagamentoRepo;
    private final ComprovanteStoragePort comprovanteStorage;

    @Override
    @Transactional
    @Auditavel(acao = "registrar_recurso", entidade = "RecursoFundoEleitoral")
    public RecursoFundoEleitoral registrarRecurso(RegistrarRecursoCommand cmd) {
        Long partidoId = tenantObrigatorio();
        return recursoRepo.save(new RecursoFundoEleitoral(
                null, partidoId, cmd.candidatoId(),
                cmd.tipoRecurso(), cmd.valor(), cmd.dataRepasse(),
                cmd.origem(), cmd.numeroDocumento(), null, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "lancar_despesa", entidade = "Despesa")
    public Despesa lancarDespesa(LancarDespesaCommand cmd) {
        Long partidoId = tenantObrigatorio();
        return despesaRepo.save(new Despesa(
                null, partidoId, cmd.candidatoId(),
                cmd.categoria(), cmd.subcategoriaTse(), cmd.valor(), cmd.data(),
                cmd.descricao(), cmd.lancadoPor(), null,
                StatusDespesa.PENDENTE, null, null, null, Instant.now()));
    }

    @Override
    @Transactional
    @Auditavel(acao = "aprovar_despesa", entidade = "Despesa")
    public Despesa aprovarDespesa(Long despesaId, Long aprovadorId) {
        Despesa d = despesaRepo.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("despesa não encontrada"));
        // Domain garante transição de estado válida (lança IllegalStateException se != PENDENTE):
        return despesaRepo.save(d.aprovar(aprovadorId));
    }

    @Override
    @Transactional
    @Auditavel(acao = "rejeitar_despesa", entidade = "Despesa")
    public Despesa rejeitarDespesa(Long despesaId, Long aprovadorId, String motivo) {
        Despesa d = despesaRepo.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("despesa não encontrada"));
        return despesaRepo.save(d.rejeitar(aprovadorId, motivo));
    }

    @Override
    @Transactional
    @Auditavel(acao = "registrar_pagamento_equipe", entidade = "PagamentoEquipe")
    public PagamentoEquipe registrarPagamentoEquipe(RegistrarPagamentoEquipeCommand cmd) {
        Long partidoId = tenantObrigatorio();
        Despesa despesa = despesaRepo.findById(cmd.despesaId())
                .orElseThrow(() -> new IllegalArgumentException("despesa não encontrada"));
        if (!despesa.categoria().exigePagamentoEquipe()) {
            throw new IllegalStateException(
                    "PagamentoEquipe só se aplica a despesa PESSOAL (categoria atual: " + despesa.categoria() + ")");
        }
        return pagamentoRepo.save(new PagamentoEquipe(
                null, partidoId, cmd.despesaId(), cmd.membroId(),
                cmd.tipoPagamento(), cmd.quantidade(), cmd.valorUnitario(),
                cmd.periodoReferencia(), Instant.now()));
    }

    @Override
    @Transactional
    public Despesa anexarComprovanteDespesa(Long despesaId, InputStream content, long contentLength,
                                            String contentType, String nomeArquivo) {
        Despesa d = despesaRepo.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("despesa não encontrada"));
        String key = "comprovantes/despesas/" + d.partidoId() + "/" + despesaId + "/"
                + UUID.randomUUID() + "-" + sanitize(nomeArquivo);
        try {
            comprovanteStorage.save(key, content, contentLength, contentType);
        } catch (IOException e) {
            throw new IllegalStateException("falha ao salvar comprovante: " + e.getMessage(), e);
        }
        return despesaRepo.save(d.comComprovante(key));
    }

    @Override
    @Transactional(readOnly = true)
    public String presignedUrlComprovanteDespesa(Long despesaId) {
        Despesa d = despesaRepo.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("despesa não encontrada"));
        if (d.comprovanteUrl() == null) {
            throw new IllegalStateException("despesa não possui comprovante anexado");
        }
        return comprovanteStorage.presignedGetUrl(d.comprovanteUrl(), Duration.ofMinutes(5));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Despesa> listarDespesas() {
        return despesaRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoFundoEleitoral> listarRecursos() {
        return recursoRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public RelatorioFinanceiroJson gerarRelatorioJson(Long candidatoId) {
        BigDecimal totalRecursos = recursoRepo.totalPorCandidato(candidatoId);
        BigDecimal totalDespesas = despesaRepo.totalAprovadoPorCandidato(candidatoId);
        List<Despesa> aprovadas = despesaRepo.findByCandidatoId(candidatoId).stream()
                .filter(d -> d.status() == StatusDespesa.APROVADO)
                .toList();
        Map<com.campanha.financeiro.domain.CategoriaDespesa, BigDecimal> porCategoria = new java.util.EnumMap<>(com.campanha.financeiro.domain.CategoriaDespesa.class);
        for (Despesa d : aprovadas) {
            porCategoria.merge(d.categoria(), d.valor(), BigDecimal::add);
        }
        List<TotalPorCategoria> lista = porCategoria.entrySet().stream()
                .map(e -> new TotalPorCategoria(e.getKey(), e.getValue()))
                .toList();
        BigDecimal saldo = totalRecursos.subtract(totalDespesas);
        return new RelatorioFinanceiroJson(candidatoId, totalRecursos, totalDespesas, saldo, lista);
    }

    private Long tenantObrigatorio() {
        Long t = TenantContext.get();
        if (t == null) {
            throw new AccessDeniedException(
                    "operação financeira requer contexto de partido (SUPER_ADMIN sem sessão de suporte não pode executar)");
        }
        return t;
    }

    private String sanitize(String name) {
        if (name == null) return "arquivo.bin";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
