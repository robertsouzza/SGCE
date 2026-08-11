package com.campanha.financeiro.infrastructure.adapter.in.web;

import com.campanha.financeiro.application.port.in.FinanceiroUseCases;
import com.campanha.financeiro.application.port.in.FinanceiroUseCases.RelatorioFinanceiroJson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/financeiro")
@RequiredArgsConstructor
public class RelatorioController {

    private final FinanceiroUseCases uc;
    private final RelatorioPdfGenerator pdfGenerator;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO','CANDIDATO')")
    public RelatorioFinanceiroJson relatorioJson(@RequestParam Long candidatoId) {
        return uc.gerarRelatorioJson(candidatoId);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE_FINANCEIRO','SECRETARIO','CANDIDATO')")
    public ResponseEntity<byte[]> relatorioPdf(@RequestParam Long candidatoId) {
        RelatorioFinanceiroJson dados = uc.gerarRelatorioJson(candidatoId);
        byte[] pdf = pdfGenerator.gerar(dados);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "relatorio-financeiro-candidato-" + candidatoId + ".pdf");
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
