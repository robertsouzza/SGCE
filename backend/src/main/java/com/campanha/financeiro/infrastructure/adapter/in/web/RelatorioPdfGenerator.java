package com.campanha.financeiro.infrastructure.adapter.in.web;

import com.campanha.financeiro.application.port.in.FinanceiroUseCases.RelatorioFinanceiroJson;
import com.campanha.financeiro.application.port.in.FinanceiroUseCases.TotalPorCategoria;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Gerador de PDF do relatório financeiro (RF-09). Layout minimalista;
 * detalhes de branding ficam para skill futura de UI.
 */
@Component
public class RelatorioPdfGenerator {

    public byte[] gerar(RelatorioFinanceiroJson dados) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font secao = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font corpo = FontFactory.getFont(FontFactory.HELVETICA, 11);

            doc.add(new Paragraph("Relatório Financeiro — SGCE", titulo));
            doc.add(new Paragraph("Candidato ID: " + dados.candidatoId(), corpo));
            doc.add(new Paragraph("Gerado em: " + Instant.now(), corpo));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Totais", secao));
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            addRow(t, "Total de recursos recebidos", brl(dados.totalRecursos()));
            addRow(t, "Total de despesas aprovadas", brl(dados.totalDespesasAprovadas()));
            addRow(t, "Saldo atual", brl(dados.saldoAtual()));
            doc.add(t);
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Despesas por categoria", secao));
            if (dados.despesasPorCategoria().isEmpty()) {
                doc.add(new Paragraph("Nenhuma despesa aprovada.", corpo));
            } else {
                PdfPTable cat = new PdfPTable(2);
                cat.setWidthPercentage(100);
                for (TotalPorCategoria tc : dados.despesasPorCategoria()) {
                    addRow(cat, tc.categoria().name(), brl(tc.total()));
                }
                doc.add(cat);
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(
                    "Este relatório complementa — não substitui — a prestação de contas oficial no SPCE do TSE.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("falha ao gerar PDF do relatório: " + e.getMessage(), e);
        }
    }

    private static void addRow(PdfPTable t, String label, String valor) {
        PdfPCell c1 = new PdfPCell(new Paragraph(label));
        PdfPCell c2 = new PdfPCell(new Paragraph(valor));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1);
        t.addCell(c2);
    }

    private static String brl(BigDecimal v) {
        if (v == null) return "R$ 0,00";
        return "R$ " + v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
