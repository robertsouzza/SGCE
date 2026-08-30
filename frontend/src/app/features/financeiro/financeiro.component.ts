import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { CategoriaDespesa, Despesa, FinanceiroService, Recurso, RelatorioJson, TipoRecurso } from './financeiro.service';

@Component({
  selector: 'sgce-financeiro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Financeiro</h1>

    <section>
      <h2>Recursos</h2>
      @if (podeLancar()) {
        <form class="card" (ngSubmit)="registrarRecurso()">
          <label>Candidato ID <input name="candidatoId" type="number" [(ngModel)]="formRecurso.candidatoId" required /></label>
          <label>Tipo
            <select name="tipoRecurso" [(ngModel)]="formRecurso.tipoRecurso">
              <option value="FUNDO_ELEITORAL">Fundo Eleitoral</option>
              <option value="FUNDO_PARTIDARIO">Fundo Partidário</option>
              <option value="DOACAO">Doação</option>
            </select>
          </label>
          <label>Valor <input name="valor" type="number" step="0.01" [(ngModel)]="formRecurso.valor" required /></label>
          <label>Data <input name="dataRepasse" type="date" [(ngModel)]="formRecurso.dataRepasse" required /></label>
          <label>Origem <input name="origem" [(ngModel)]="formRecurso.origem" /></label>
          <label>Nº documento <input name="numeroDocumento" [(ngModel)]="formRecurso.numeroDocumento" /></label>
          <button type="submit">Registrar recurso</button>
        </form>
      }
      <table style="margin-top: 16px;">
        <thead><tr><th>ID</th><th>Candidato</th><th>Tipo</th><th>Valor</th><th>Data</th></tr></thead>
        <tbody>
          @for (r of recursos(); track r.id) {
            <tr><td>{{ r.id }}</td><td>{{ r.candidatoId }}</td><td>{{ r.tipoRecurso }}</td><td>{{ r.valor | number:'1.2-2' }}</td><td>{{ r.dataRepasse }}</td></tr>
          }
        </tbody>
      </table>
    </section>

    <section style="margin-top: 40px;">
      <h2>Despesas</h2>
      @if (podeLancar()) {
        <form class="card" (ngSubmit)="lancarDespesa()">
          <label>Candidato ID <input name="candDespesa" type="number" [(ngModel)]="formDespesa.candidatoId" required /></label>
          <label>Categoria
            <select name="categoria" [(ngModel)]="formDespesa.categoria">
              <option value="PESSOAL">Pessoal</option>
              <option value="ALIMENTACAO">Alimentação</option>
              <option value="TRANSPORTE">Transporte</option>
              <option value="MATERIAL_GRAFICO">Material gráfico</option>
              <option value="OUTROS">Outros</option>
            </select>
          </label>
          <label>Valor <input name="valorDespesa" type="number" step="0.01" [(ngModel)]="formDespesa.valor" required /></label>
          <label>Data <input name="dataDespesa" type="date" [(ngModel)]="formDespesa.data" required /></label>
          <label>Descrição <input name="descricao" [(ngModel)]="formDespesa.descricao" /></label>
          <label>Comprovante (PDF/imagem) <input name="arquivo" type="file" (change)="onArquivo($event)" /></label>
          <button type="submit">Lançar despesa</button>
        </form>
      }
      <table style="margin-top: 16px;">
        <thead><tr><th>ID</th><th>Cand.</th><th>Categoria</th><th>Valor</th><th>Data</th><th>Status</th><th>Ações</th></tr></thead>
        <tbody>
          @for (d of despesas(); track d.id) {
            <tr>
              <td>{{ d.id }}</td><td>{{ d.candidatoId }}</td><td>{{ d.categoria }}</td>
              <td>{{ d.valor | number:'1.2-2' }}</td><td>{{ d.data }}</td>
              <td><span class="badge" [class]="'badge ' + d.status.toLowerCase()">{{ d.status }}</span></td>
              <td>
                @if (d.status === 'PENDENTE' && podeAprovar()) {
                  <button (click)="aprovar(d)">Aprovar</button>
                }
                @if (d.comprovanteUrl) {
                  <button (click)="verComprovante(d.id)">Ver comprovante</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </section>

    <section style="margin-top: 40px;">
      <h2>Relatório financeiro</h2>
      <form (ngSubmit)="carregarRelatorio()" style="display: flex; gap: 8px; align-items: center;">
        <input name="relCand" type="number" [(ngModel)]="relCandidatoId" placeholder="Candidato ID" />
        <button type="submit">Ver JSON</button>
        <a [href]="pdfUrl()" target="_blank" rel="noopener">Baixar PDF</a>
      </form>
      @if (relatorio(); as r) {
        <div class="card" style="margin-top: 16px;">
          <p><strong>Total de recursos:</strong> {{ r.totalRecursos | number:'1.2-2' }}</p>
          <p><strong>Total de despesas aprovadas:</strong> {{ r.totalDespesasAprovadas | number:'1.2-2' }}</p>
          <p><strong>Saldo atual:</strong> {{ r.saldoAtual | number:'1.2-2' }}</p>
          <h4>Por categoria</h4>
          <ul>
            @for (dc of r.despesasPorCategoria; track dc.categoria) {
              <li>{{ dc.categoria }}: {{ dc.total | number:'1.2-2' }}</li>
            }
          </ul>
        </div>
      }
    </section>
  `,
})
export class FinanceiroComponent {
  private svc = inject(FinanceiroService);
  private auth = inject(AuthService);

  recursos = signal<Recurso[]>([]);
  despesas = signal<Despesa[]>([]);
  relatorio = signal<RelatorioJson | null>(null);
  relCandidatoId = 0;
  pdfUrl = computed(() => this.svc.relatorioPdfUrl(this.relCandidatoId || 0));

  formRecurso = { candidatoId: 0, tipoRecurso: 'FUNDO_ELEITORAL' as TipoRecurso, valor: 0, dataRepasse: '', origem: '', numeroDocumento: '' };
  formDespesa = { candidatoId: 0, categoria: 'OUTROS' as CategoriaDespesa, valor: 0, data: '', descricao: '' };
  arquivoComprovante: File | null = null;

  podeLancar = computed(() => {
    const p = this.auth.perfil();
    return p === 'ADMIN' || p === 'GERENTE_FINANCEIRO' || p === 'SECRETARIO';
  });
  podeAprovar = computed(() => {
    const p = this.auth.perfil();
    return p === 'ADMIN' || p === 'GERENTE_FINANCEIRO';
  });

  constructor() { this.recarregar(); }

  recarregar(): void {
    this.svc.listarRecursos().subscribe(rs => this.recursos.set(rs));
    this.svc.listarDespesas().subscribe(ds => this.despesas.set(ds));
  }

  registrarRecurso(): void {
    this.svc.criarRecurso(this.formRecurso as Omit<Recurso, 'id'>).subscribe(() => {
      this.formRecurso = { candidatoId: 0, tipoRecurso: 'FUNDO_ELEITORAL', valor: 0, dataRepasse: '', origem: '', numeroDocumento: '' };
      this.recarregar();
    });
  }

  onArquivo(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.arquivoComprovante = input.files?.[0] ?? null;
  }

  lancarDespesa(): void {
    this.svc.criarDespesa(this.formDespesa).subscribe(d => {
      if (this.arquivoComprovante) {
        this.svc.anexarComprovante(d.id, this.arquivoComprovante).subscribe(() => {
          this.arquivoComprovante = null;
          this.recarregar();
        });
      } else {
        this.recarregar();
      }
      this.formDespesa = { candidatoId: 0, categoria: 'OUTROS', valor: 0, data: '', descricao: '' };
    });
  }

  aprovar(d: Despesa): void {
    this.svc.aprovar(d.id).subscribe(() => this.recarregar());
  }

  verComprovante(id: number): void {
    this.svc.presignedUrl(id).subscribe(r => window.open(r.url, '_blank'));
  }

  carregarRelatorio(): void {
    if (!this.relCandidatoId) return;
    this.svc.relatorioJson(this.relCandidatoId).subscribe(r => this.relatorio.set(r));
  }
}
