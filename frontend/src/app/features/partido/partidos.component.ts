import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Partido, PartidoService } from './partido.service';

interface Filtros {
  nome: string;
  sigla: string;
  numeroPartido: string;
  status: 'ativos' | 'inativos' | 'todos';
}

@Component({
  selector: 'sgce-partidos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Partidos</h1>
        <a routerLink="/partidos/novo" class="btn primary">+ Novo partido</a>
      </header>

      <section class="filtros card">
        <div class="grid">
          <label>
            Nome
            <input type="text" [ngModel]="filtros().nome"
                   (ngModelChange)="alterarFiltro('nome', $event)"
                   placeholder="Pesquisar por nome" />
          </label>
          <label>
            Sigla
            <input type="text" [ngModel]="filtros().sigla"
                   (ngModelChange)="alterarFiltro('sigla', $event)"
                   placeholder="Ex: PT, PDT" />
          </label>
          <label>
            Número
            <input type="text" [ngModel]="filtros().numeroPartido"
                   (ngModelChange)="alterarFiltro('numeroPartido', $event)"
                   placeholder="Ex: 13" />
          </label>
          <label>
            Status
            <select [ngModel]="filtros().status" (ngModelChange)="alterarFiltro('status', $event)">
              <option value="ativos">Somente ativos</option>
              <option value="inativos">Somente inativos</option>
              <option value="todos">Todos</option>
            </select>
          </label>
        </div>
        <button type="button" class="btn ghost" (click)="limparFiltros()">Limpar filtros</button>
      </section>

      <div class="resumo">
        Mostrando <strong>{{ paginaAtualItens().length }}</strong> de {{ filtrados().length }}
        (total: {{ partidos().length }})
      </div>

      <table class="lista">
        <thead>
          <tr>
            <th>ID</th><th>Nome</th><th>Sigla</th><th>Número</th><th>CNPJ</th>
            <th>Status</th><th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (p of paginaAtualItens(); track p.id) {
            <tr [class.inativo]="!p.ativo">
              <td>{{ p.id }}</td>
              <td>{{ p.nome }}</td>
              <td>{{ p.sigla }}</td>
              <td>{{ p.numeroPartido }}</td>
              <td>{{ p.cnpj }}</td>
              <td>
                <span class="badge" [class.ok]="p.ativo" [class.off]="!p.ativo">
                  {{ p.ativo ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
              <td class="acoes">
                <a [routerLink]="['/partidos', p.id, 'editar']" class="btn">Editar</a>
                @if (p.ativo) {
                  <button type="button" class="btn warn" (click)="inativar(p)" [disabled]="processando() === p.id">Inativar</button>
                } @else {
                  <button type="button" class="btn ok" (click)="reativar(p)" [disabled]="processando() === p.id">Reativar</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="vazio">Nenhum partido encontrado com os filtros atuais.</td></tr>
          }
        </tbody>
      </table>

      @if (totalPaginas() > 1) {
        <nav class="paginacao">
          <button type="button" (click)="mudarPagina(pagina() - 1)" [disabled]="pagina() === 1">← Anterior</button>
          @for (p of numerosPagina(); track p) {
            <button type="button" [class.ativa]="p === pagina()" (click)="mudarPagina(p)">{{ p }}</button>
          }
          <button type="button" (click)="mudarPagina(pagina() + 1)" [disabled]="pagina() === totalPaginas()">Próxima →</button>
        </nav>
      }

      @if (mensagem()) {
        <div class="mensagem" [class.erro]="erroMsg()">{{ mensagem() }}</div>
      }
    </div>
  `,
  styles: [
    `
      .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
      .filtros.card { background: #fff; padding: 16px; border-radius: 6px; margin-bottom: 12px; }
      .filtros .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin-bottom: 8px; }
      .filtros label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #475569; }
      .filtros input, .filtros select { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .resumo { color: #64748b; font-size: 13px; margin-bottom: 8px; }
      table.lista { width: 100%; background: #fff; border-collapse: collapse; border-radius: 6px; overflow: hidden; }
      table.lista th, table.lista td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e2e8f0; }
      table.lista th { background: #f1f5f9; font-size: 13px; color: #475569; }
      tr.inativo td { opacity: 0.55; }
      .badge { padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
      .badge.ok { background: #dcfce7; color: #166534; }
      .badge.off { background: #f1f5f9; color: #64748b; }
      .acoes { white-space: nowrap; display: flex; gap: 6px; }
      .vazio { text-align: center; padding: 24px; color: #94a3b8; }
      .btn { padding: 6px 12px; border: 1px solid #cbd5e1; background: #f1f5f9; border-radius: 4px; cursor: pointer; font-size: 13px; text-decoration: none; color: #1e293b; display: inline-block; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn.warn { background: #f59e0b; color: #fff; border-color: #f59e0b; }
      .btn.ok { background: #16a34a; color: #fff; border-color: #16a34a; }
      .btn.ghost { background: transparent; color: #475569; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .paginacao { display: flex; gap: 4px; margin-top: 12px; justify-content: center; }
      .paginacao button { min-width: 36px; padding: 4px 10px; border: 1px solid #cbd5e1; background: #fff; border-radius: 4px; cursor: pointer; }
      .paginacao button.ativa { background: #2563eb; color: #fff; border-color: #2563eb; }
      .paginacao button:disabled { opacity: 0.4; cursor: not-allowed; }
      .mensagem { margin-top: 12px; padding: 10px; border-radius: 4px; background: #dcfce7; color: #166534; }
      .mensagem.erro { background: #fee2e2; color: #991b1b; }
    `,
  ],
})
export class PartidosComponent {
  private svc = inject(PartidoService);

  partidos = signal<Partido[]>([]);
  processando = signal<number | null>(null);
  mensagem = signal<string | null>(null);
  erroMsg = signal(false);
  pagina = signal(1);
  private readonly TAMANHO_PAGINA = 10;

  filtros = signal<Filtros>({ nome: '', sigla: '', numeroPartido: '', status: 'ativos' });

  filtrados = computed<Partido[]>(() => {
    const f = this.filtros();
    return this.partidos().filter(p => {
      if (f.status === 'ativos' && !p.ativo) return false;
      if (f.status === 'inativos' && p.ativo) return false;
      if (f.nome && !p.nome.toLowerCase().includes(f.nome.toLowerCase())) return false;
      if (f.sigla && !p.sigla.toLowerCase().includes(f.sigla.toLowerCase())) return false;
      if (f.numeroPartido && !String(p.numeroPartido).includes(f.numeroPartido.trim())) return false;
      return true;
    });
  });

  totalPaginas = computed(() => Math.max(1, Math.ceil(this.filtrados().length / this.TAMANHO_PAGINA)));
  paginaAtualItens = computed(() => {
    const inicio = (this.pagina() - 1) * this.TAMANHO_PAGINA;
    return this.filtrados().slice(inicio, inicio + this.TAMANHO_PAGINA);
  });
  numerosPagina = computed(() => {
    const total = this.totalPaginas(), atual = this.pagina(), janela = 5;
    const inicio = Math.max(1, atual - Math.floor(janela / 2));
    const fim = Math.min(total, inicio + janela - 1);
    const nums: number[] = [];
    for (let i = inicio; i <= fim; i++) nums.push(i);
    return nums;
  });

  constructor() { this.recarregar(); }

  recarregar(): void {
    this.svc.listar().subscribe({
      next: ps => this.partidos.set(ps),
      error: () => this.mostrarErro('Falha ao carregar partidos.'),
    });
  }

  alterarFiltro<K extends keyof Filtros>(campo: K, valor: Filtros[K]): void {
    this.filtros.update(f => ({ ...f, [campo]: valor }));
    this.pagina.set(1);
  }

  limparFiltros(): void {
    this.filtros.set({ nome: '', sigla: '', numeroPartido: '', status: 'ativos' });
    this.pagina.set(1);
  }

  mudarPagina(p: number): void {
    if (p < 1 || p > this.totalPaginas()) return;
    this.pagina.set(p);
  }

  async inativar(p: Partido): Promise<void> {
    if (!confirm(`Inativar o partido "${p.nome}" (${p.sigla})?`)) return;
    this.processando.set(p.id);
    try {
      const atualizado = await firstValueFrom(this.svc.inativar(p.id));
      this.substituir(atualizado);
      this.mostrarOk(`Partido "${p.sigla}" inativado.`);
    } catch (e) {
      this.mostrarErro('Falha ao inativar: ' + (this.extrairMensagem(e) ?? 'erro inesperado'));
    } finally {
      this.processando.set(null);
    }
  }

  async reativar(p: Partido): Promise<void> {
    this.processando.set(p.id);
    try {
      const atualizado = await firstValueFrom(this.svc.reativar(p.id));
      this.substituir(atualizado);
      this.mostrarOk(`Partido "${p.sigla}" reativado.`);
    } catch (e) {
      this.mostrarErro('Falha ao reativar: ' + (this.extrairMensagem(e) ?? 'erro inesperado'));
    } finally {
      this.processando.set(null);
    }
  }

  private substituir(p: Partido): void {
    this.partidos.update(list => list.map(x => (x.id === p.id ? p : x)));
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

  private extrairMensagem(e: unknown): string | null {
    const err = e as { error?: { mensagem?: string } };
    return err?.error?.mensagem ?? null;
  }
}
