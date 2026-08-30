import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoRecurso = 'FUNDO_ELEITORAL' | 'FUNDO_PARTIDARIO' | 'DOACAO';
export type CategoriaDespesa = 'PESSOAL' | 'ALIMENTACAO' | 'TRANSPORTE' | 'MATERIAL_GRAFICO' | 'OUTROS';
export type StatusDespesa = 'PENDENTE' | 'APROVADO' | 'REJEITADO';

export interface Recurso {
  id: number;
  candidatoId: number;
  tipoRecurso: TipoRecurso;
  valor: number;
  dataRepasse: string;
  origem?: string;
  numeroDocumento?: string;
}
export interface Despesa {
  id: number;
  candidatoId: number;
  categoria: CategoriaDespesa;
  valor: number;
  data: string;
  descricao?: string;
  status: StatusDespesa;
  comprovanteUrl?: string;
  aprovadoPor?: number;
  aprovadoEm?: string;
  motivoRejeicao?: string;
}
export interface RelatorioJson {
  candidatoId: number;
  totalRecursos: number;
  totalDespesasAprovadas: number;
  saldoAtual: number;
  despesasPorCategoria: { categoria: CategoriaDespesa; total: number }[];
}

@Injectable({ providedIn: 'root' })
export class FinanceiroService {
  private http = inject(HttpClient);

  listarRecursos(): Observable<Recurso[]> { return this.http.get<Recurso[]>('/api/recursos'); }
  criarRecurso(input: Omit<Recurso, 'id'>): Observable<Recurso> { return this.http.post<Recurso>('/api/recursos', input); }

  listarDespesas(): Observable<Despesa[]> { return this.http.get<Despesa[]>('/api/despesas'); }
  criarDespesa(input: { candidatoId: number; categoria: CategoriaDespesa; valor: number; data: string; descricao?: string }): Observable<Despesa> {
    return this.http.post<Despesa>('/api/despesas', input);
  }
  aprovar(id: number): Observable<Despesa> { return this.http.patch<Despesa>(`/api/despesas/${id}/aprovar`, {}); }
  rejeitar(id: number, motivo: string): Observable<Despesa> { return this.http.patch<Despesa>(`/api/despesas/${id}/rejeitar`, { motivo }); }
  anexarComprovante(id: number, arquivo: File): Observable<Despesa> {
    const fd = new FormData();
    fd.append('arquivo', arquivo);
    return this.http.post<Despesa>(`/api/despesas/${id}/comprovante`, fd);
  }
  presignedUrl(id: number): Observable<{ url: string }> { return this.http.get<{ url: string }>(`/api/despesas/${id}/comprovante-url`); }

  relatorioJson(candidatoId: number): Observable<RelatorioJson> {
    return this.http.get<RelatorioJson>(`/api/relatorios/financeiro?candidatoId=${candidatoId}`);
  }
  relatorioPdfUrl(candidatoId: number): string {
    return `/api/relatorios/financeiro/pdf?candidatoId=${candidatoId}`;
  }
}
