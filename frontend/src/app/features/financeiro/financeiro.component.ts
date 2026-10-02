import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import {
  CategoriaDespesa,
  Despesa,
  FinanceiroService,
  Recurso,
  RelatorioJson,
  StatusDespesa,
  TipoRecurso,
} from './financeiro.service';

interface FiltrosRecurso {
  tipo: TipoRecurso | '';
  candidatoId: string;
  status: 'ativos' | 'inativos' | 'todos';
}
interface FiltrosDespesa {
  categoria: CategoriaDespesa | '';
  status: StatusDespesa | '';
  candidatoId: string;
  dataInicio: string;
  dataFim: string;
}

@Component({
  selector: 'sgce-financeiro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <h1>Financeiro</h1>

    <!-- Recursos -->
    <section class="bloco">
      <header class="page-header">
        <h2>Recursos</h2>
        @if (podeLancar()) {
          <a routerLink="/financeiro/recursos/novo" class="btn primary">+ Novo recurso</a>
        }
      </header>

      <section class="filtros card">
        <div class="grid">
          <label>Tipo
            <select [ngModel]="fRec().tipo" (ngModelChange)="alterarFiltroRec('tipo', $event)">
              <option value="">Todos</option>
              <option value="FUNDO_ELEITORAL">Fundo Eleitoral</option>
              <option value="FUNDO_PARTIDARIO">Fundo Partidário</option>
              <option value="DOACAO">Doação</option>
            </select>
          </label>
          <label>Candidato (ID)
            <input type="text" [ngModel]="fRec().candidatoId" (ngModelChange)="alterarFiltroRec('candidatoId', $event)" placeholder="Ex: 14" />
          </label>
          <label>Status
            <select [ngModel]="fRec().status" (ngModelChange)="alterarFiltroRec('status', $event)">
              <option value="ativos">Somente ativos</option>
              <option value="inativos">Somente inativos</option>
              <option value="todos">Todos</option>
            </select>
          </label>
        </div>
      </section>

      <div class="resumo">
        Mostrando <strong>{{ recPagina().length }}</strong> de {{ recFiltrados().length }}
        (total: {{ recursos().length }})
      </div>

      <table class="lista">
        <thead>
          <tr><th>ID</th><th>Candidato</th><th>Tipo</th><th>Valor</th><th>Data</th><th>Status</th><th>Ações</th></tr>
        </thead>
        <tbody>
          @for (r of recPagina(); track r.id) {
            <tr [class.inativo]="!r.ativo">
              <td>{{ r.id }}</td>
              <td>#{{ r.candidatoId }}</td>
              <td>{{ r.tipoRecurso }}</td>
              <td>{{ r.valor | number:'1.2-2' }}</td>
              <td>{{ r.dataRepasse }}</td>
              <td><span class="badge" [class.ok]="r.ativo" [class.off]="!r.ativo">{{ r.ativo ? 'Ativo' : 'Inativo' }}</span></td>
              <td class="acoes">
                @if (podeLancar()) {
                  <a [routerLink]="['/financeiro/recursos', r.id, 'editar']" class="btn">Editar</a>
                  @if (r.ativo) {
                    <button type="button" class="btn warn" (click)="inativarRec(r)" [disabled]="procRec() === r.id">Inativar</button>
                  } @else {
                    <button type="button" class="btn ok" (click)="reativarRec(r)" [disabled]="procRec() === r.id">Reativar</button>
                  }
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="vazio">Nenhum recurso encontrado.</td></tr>
          }
        </tbody>
      </table>

      @if (recTotalPaginas() > 1) {
        <nav class="paginacao">
          <button (click)="recPaginaAtual.set(recPaginaAtual() - 1)" [disabled]="recPaginaAtual() === 1">←</button>
          <span>Página {{ recPaginaAtual() }} de {{ recTotalPaginas() }}</span>
          <button (click)="recPaginaAtual.set(recPaginaAtual() + 1)" [disabled]="recPaginaAtual() === recTotalPaginas()">→</button>
        </nav>
      }
    </section>

    <!-- Despesas -->
    <section class="bloco" style="margin-top: 32px;">
      <header class="page-header">
        <h2>Despesas</h2>
        @if (podeLancar()) {
          <a routerLink="/financeiro/despesas/nova" class="btn primary">+ Lançar despesa</a>
        }
      </header>

      <section class="filtros card">
        <div class="grid">
          <label>Categoria
            <select [ngModel]="fDes().categoria" (ngModelChange)="alterarFiltroDes('categoria', $event)">
              <option value="">Todas</option>
              <option value="PESSOAL">Pessoal</option>
              <option value="ALIMENTACAO">Alimentação</option>
              <option value="TRANSPORTE">Transporte</option>
              <option value="MATERIAL_GRAFICO">Material gráfico</option>
              <option value="OUTROS">Outros</option>
            </select>
          </label>
          <label>Status
            <select [ngModel]="fDes().status" (ngModelChange)="alterarFiltroDes('status', $event)">
              <option value="">Todos</option>
              <option value="PENDENTE">Pendente</option>
              <option value="APROVADO">Aprovado</option>
              <option value="REJEITADO">Rejeitado</option>
            </select>
          </label>
          <label>Candidato (ID)
            <input type="text" [ngModel]="fDes().candidatoId" (ngModelChange)="alterarFiltroDes('candidatoId', $event)" placeholder="Ex: 14" />
          </label>
          <label>De (data)
            <input type="date" [ngModel]="fDes().dataInicio" (ngModelChange)="alterarFiltroDes('dataInicio', $event)" />
          </label>
          <label>Até (data)
            <input type="date" [ngModel]="fDes().dataFim" (ngModelChange)="alterarFiltroDes('dataFim', $event)" />
          </label>
        </div>
      </section>

      <div class="resumo">
        Mostrando <strong>{{ desPagina().length }}</strong> de {{ desFiltrados().length }}
        (total: {{ despesas().length }})
      </div>

      <table class="lista">
        <thead>
          <tr><th>ID</th><th>Cand.</th><th>Categoria</th><th>Valor</th><th>Data</th><th>Status</th><th>Ações</th></tr>
        </thead>
        <tbody>
          @for (d of desPagina(); track d.id) {
            <tr>
              <td>{{ d.id }}</td>
              <td>#{{ d.candidatoId }}</td>
              <td>{{ d.categoria }}</td>
              <td>{{ d.valor | number:'1.2-2' }}</td>
              <td>{{ d.data }}</td>
              <td><span class="badge" [class]="'badge ' + d.status.toLowerCase()">{{ d.status }}</span></td>
              <td class="acoes">
                @if (d.status === 'PENDENTE' && podeAprovar()) {
                  <button type="button" class="btn ok" (click)="aprovar(d)">Aprovar</button>
                  <button type="button" class="btn warn" (click)="rejeitarPrompt(d)">Rejeitar</button>
                }
                @if (d.comprovanteUrl) {
                  <button type="button" class="btn" (click)="verComprovante(d.id)">Comprovante</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="vazio">Nenhuma despesa encontrada.</td></tr>
          }
        </tbody>
      </table>

      @if (desTotalPaginas() > 1) {
        <nav class="paginacao">
          <button (click)="desPaginaAtual.set(desPaginaAtual() - 1)" [disabled]="desPaginaAtual() === 1">←</button>
          <span>Página {{ desPaginaAtual() }} de {{ desTotalPaginas() }}</span>
          <button (click)="desPaginaAtual.set(desPaginaAtual() + 1)" [disabled]="desPaginaAtual() === desTotalPaginas()">→</button>
        </nav>
      }
    </section>

    <!-- Relatório -->
    <section class="bloco" style="margin-top: 32px;">
      <h2>Relatório financeiro por candidato</h2>
      <form class="card inline" (ngSubmit)="carregarRelatorio()">
        <input type="number" [(ngModel)]="relCandidatoId" name="relCand" placeholder="Candidato ID" />
        <button type="submit" class="btn primary">Ver JSON</button>
        <a [href]="pdfUrl()" target="_blank" rel="noopener" class="btn">Baixar PDF</a>
      </form>
      @if (relatorio(); as r) {
        <div class="card" style="margin-top: 12px;">
          <p><strong>Total de recursos:</strong> R$ {{ r.totalRecursos | number:'1.2-2' }}</p>
          <p><strong>Total de despesas aprovadas:</strong> R$ {{ r.totalDespesasAprovadas | number:'1.2-2' }}</p>
          <p><strong>Saldo atual:</strong> R$ {{ r.saldoAtual | number:'1.2-2' }}</p>
          <h4>Por categoria</h4>
          <ul>
            @for (dc of r.despesasPorCategoria; track dc.categoria) {
              <li>{{ dc.categoria }}: R$ {{ dc.total | number:'1.2-2' }}</li>
            }
          </ul>
        </div>
      }
    </section>

    @if (mensagem()) {
      <div class="mensagem" [class.erro]="erroMsg()">{{ mensagem() }}</div>
    }
  `,
  styles: [
    `
      h1, h2 { margin: 0; }
      .bloco { margin-top: 16px; }
      .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
      .filtros.card { background: #fff; padding: 14px; border-radius: 6px; margin-bottom: 10px; }
      .filtros .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 10px; }
      .filtros label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #475569; }
      .filtros input, .filtros select { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .resumo { color: #64748b; font-size: 13px; margin-bottom: 8px; }
      table.lista { width: 100%; background: #fff; border-collapse: collapse; border-radius: 6px; overflow: hidden; }
      table.lista th, table.lista td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 13px; }
      table.lista th { background: #f1f5f9; color: #475569; }
      tr.inativo td { opacity: 0.55; }
      .badge { padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
      .badge.ok, .badge.aprovado { background: #dcfce7; color: #166534; }
      .badge.off { background: #f1f5f9; color: #64748b; }
      .badge.pendente { background: #fef3c7; color: #92400e; }
      .badge.rejeitado { background: #fee2e2; color: #991b1b; }
      .acoes { white-space: nowrap; display: flex; gap: 4px; }
      .vazio { text-align: center; padding: 20px; color: #94a3b8; }
      .btn { padding: 4px 10px; border: 1px solid #cbd5e1; background: #f1f5f9; border-radius: 4px; cursor: pointer; font-size: 12px; text-decoration: none; color: #1e293b; display: inline-block; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn.warn { background: #f59e0b; color: #fff; border-color: #f59e0b; }
      .btn.ok { background: #16a34a; color: #fff; border-color: #16a34a; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .paginacao { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 10px; font-size: 13px; }
      .paginacao button { padding: 4px 10px; border: 1px solid #cbd5e1; background: #fff; border-radius: 4px; cursor: pointer; }
      .paginacao button:disabled { opacity: 0.4; cursor: not-allowed; }
      .card.inline { display: flex; gap: 8px; align-items: center; background: #fff; padding: 10px; border-radius: 6px; }
      .card.inline input { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .mensagem { margin-top: 12px; padding: 10px; border-radius: 4px; background: #dcfce7; color: #166534; }
      .mensagem.erro { background: #fee2e2; color: #991b1b; }
    `,
  ],
})
export class FinanceiroComponent {
  private svc = inject(FinanceiroService);
  private auth = inject(AuthService);

  recursos = signal<Recurso[]>([]);
  despesas = signal<Despesa[]>([]);
  relatorio = signal<RelatorioJson | null>(null);
  relCandidatoId = 0;
  procRec = signal<number | null>(null);
  mensagem = signal<string | null>(null);
  erroMsg = signal(false);

  pdfUrl = computed(() => this.svc.relatorioPdfUrl(this.relCandidatoId || 0));
  podeLancar = computed(() => ['ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO'].includes(this.auth.perfil() ?? ''));
  podeAprovar = computed(() => ['ADMIN', 'GERENTE_FINANCEIRO'].includes(this.auth.perfil() ?? ''));

  // Filtros + paginação Recursos
  fRec = signal<FiltrosRecurso>({ tipo: '', candidatoId: '', status: 'ativos' });
  recPaginaAtual = signal(1);
  private TAM = 10;
  recFiltrados = computed<Recurso[]>(() => {
    const f = this.fRec();
    return this.recursos().filter(r => {
      if (f.status === 'ativos' && !r.ativo) return false;
      if (f.status === 'inativos' && r.ativo) return false;
      if (f.tipo && r.tipoRecurso !== f.tipo) return false;
      if (f.candidatoId && !String(r.candidatoId).includes(f.candidatoId.trim())) return false;
      return true;
    });
  });
  recTotalPaginas = computed(() => Math.max(1, Math.ceil(this.recFiltrados().length / this.TAM)));
  recPagina = computed(() => {
    const i = (this.recPaginaAtual() - 1) * this.TAM;
    return this.recFiltrados().slice(i, i + this.TAM);
  });

  // Filtros + paginação Despesas
  fDes = signal<FiltrosDespesa>({ categoria: '', status: '', candidatoId: '', dataInicio: '', dataFim: '' });
  desPaginaAtual = signal(1);
  desFiltrados = computed<Despesa[]>(() => {
    const f = this.fDes();
    return this.despesas().filter(d => {
      if (f.categoria && d.categoria !== f.categoria) return false;
      if (f.status && d.status !== f.status) return false;
      if (f.candidatoId && !String(d.candidatoId).includes(f.candidatoId.trim())) return false;
      if (f.dataInicio && d.data < f.dataInicio) return false;
      if (f.dataFim && d.data > f.dataFim) return false;
      return true;
    });
  });
  desTotalPaginas = computed(() => Math.max(1, Math.ceil(this.desFiltrados().length / this.TAM)));
  desPagina = computed(() => {
    const i = (this.desPaginaAtual() - 1) * this.TAM;
    return this.desFiltrados().slice(i, i + this.TAM);
  });

  constructor() { this.recarregar(); }

  alterarFiltroRec<K extends keyof FiltrosRecurso>(c: K, v: FiltrosRecurso[K]): void {
    this.fRec.update(f => ({ ...f, [c]: v }));
    this.recPaginaAtual.set(1);
  }
  alterarFiltroDes<K extends keyof FiltrosDespesa>(c: K, v: FiltrosDespesa[K]): void {
    this.fDes.update(f => ({ ...f, [c]: v }));
    this.desPaginaAtual.set(1);
  }

  recarregar(): void {
    this.svc.listarRecursos().subscribe({ next: rs => this.recursos.set(rs), error: () => this.mostrarErro('Falha ao carregar recursos.') });
    this.svc.listarDespesas().subscribe({ next: ds => this.despesas.set(ds), error: () => this.mostrarErro('Falha ao carregar despesas.') });
  }

  async inativarRec(r: Recurso): Promise<void> {
    if (!confirm(`Inativar o recurso #${r.id}?`)) return;
    this.procRec.set(r.id);
    try {
      const atualizado = await firstValueFrom(this.svc.inativarRecurso(r.id));
      this.recursos.update(list => list.map(x => x.id === r.id ? atualizado : x));
      this.mostrarOk(`Recurso #${r.id} inativado.`);
    } catch (e) { this.mostrarErro('Falha ao inativar: ' + (this.extrai(e) ?? '')); }
    finally { this.procRec.set(null); }
  }

  async reativarRec(r: Recurso): Promise<void> {
    this.procRec.set(r.id);
    try {
      const atualizado = await firstValueFrom(this.svc.reativarRecurso(r.id));
      this.recursos.update(list => list.map(x => x.id === r.id ? atualizado : x));
      this.mostrarOk(`Recurso #${r.id} reativado.`);
    } catch (e) { this.mostrarErro('Falha ao reativar: ' + (this.extrai(e) ?? '')); }
    finally { this.procRec.set(null); }
  }

  async aprovar(d: Despesa): Promise<void> {
    try {
      const atualizada = await firstValueFrom(this.svc.aprovar(d.id));
      this.despesas.update(list => list.map(x => x.id === d.id ? atualizada : x));
      this.mostrarOk(`Despesa #${d.id} aprovada.`);
    } catch (e) { this.mostrarErro('Falha ao aprovar: ' + (this.extrai(e) ?? '')); }
  }

  async rejeitarPrompt(d: Despesa): Promise<void> {
    const motivo = prompt('Motivo da rejeição:');
    if (!motivo) return;
    try {
      const atualizada = await firstValueFrom(this.svc.rejeitar(d.id, motivo));
      this.despesas.update(list => list.map(x => x.id === d.id ? atualizada : x));
      this.mostrarOk(`Despesa #${d.id} rejeitada.`);
    } catch (e) { this.mostrarErro('Falha ao rejeitar: ' + (this.extrai(e) ?? '')); }
  }

  verComprovante(id: number): void {
    this.svc.presignedUrl(id).subscribe(r => window.open(r.url, '_blank'));
  }

  carregarRelatorio(): void {
    if (!this.relCandidatoId) return;
    this.svc.relatorioJson(this.relCandidatoId).subscribe(r => this.relatorio.set(r));
  }

  private mostrarOk(msg: string): void {
    this.erroMsg.set(false);
    this.mensagem.set(msg);
    setTimeout(() => this.mensagem.set(null), 3500);
  }
  private mostrarErro(msg: string): void {
    this.erroMsg.set(true);
    this.mensagem.set(msg);
  }
  private extrai(e: unknown): string | null {
    const err = e as { error?: { mensagem?: string } };
    return err?.error?.mensagem ?? null;
  }
}
