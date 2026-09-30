import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Cargo, Candidato, CARGOS, PartidoService, UFS } from './partido.service';

interface Filtros {
  nome: string;
  cargo: Cargo | '';
  uf: string;
  municipio: string;
  status: 'ativos' | 'inativos' | 'todos';
}

@Component({
  selector: 'sgce-candidatos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Candidatos</h1>
        <a routerLink="/candidatos/novo" class="btn primary">+ Novo candidato</a>
      </header>

      <section class="filtros card">
        <div class="grid">
          <label>
            Nome
            <input type="text" [(ngModel)]="filtros.nome" placeholder="Pesquisar por nome" (input)="resetPagina()" />
          </label>
          <label>
            Cargo
            <select [(ngModel)]="filtros.cargo" (change)="resetPagina()">
              <option value="">Todos</option>
              @for (c of cargos; track c.valor) {
                <option [value]="c.valor">{{ c.rotulo }}</option>
              }
            </select>
          </label>
          <label>
            UF
            <select [(ngModel)]="filtros.uf" (change)="resetPagina()">
              <option value="">Todas</option>
              @for (u of ufs; track u.sigla) {
                <option [value]="u.sigla">{{ u.sigla }} — {{ u.nome }}</option>
              }
            </select>
          </label>
          <label>
            Município
            <input type="text" [(ngModel)]="filtros.municipio" placeholder="Pesquisar por município" (input)="resetPagina()" />
          </label>
          <label>
            Status
            <select [(ngModel)]="filtros.status" (change)="resetPagina()">
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
        (total cadastrado: {{ candidatos().length }})
      </div>

      <table class="lista">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Cargo</th>
            <th>UF</th>
            <th>Município</th>
            <th>Status</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (c of paginaAtualItens(); track c.id) {
            <tr [class.inativo]="!c.ativo">
              <td>{{ c.id }}</td>
              <td>{{ c.nomeCompleto }}</td>
              <td>{{ rotuloCargo(c.cargo) }}</td>
              <td>{{ c.uf }}</td>
              <td>{{ c.municipio || '—' }}</td>
              <td>
                <span class="badge" [class.ok]="c.ativo" [class.off]="!c.ativo">
                  {{ c.ativo ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
              <td class="acoes">
                @if (c.ativo) {
                  <button type="button" class="btn warn" (click)="inativar(c)" [disabled]="processando() === c.id">
                    Inativar
                  </button>
                } @else {
                  <button type="button" class="btn ok" (click)="reativar(c)" [disabled]="processando() === c.id">
                    Reativar
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="7" class="vazio">Nenhum candidato encontrado com os filtros atuais.</td>
            </tr>
          }
        </tbody>
      </table>

      @if (totalPaginas() > 1) {
        <nav class="paginacao">
          <button type="button" (click)="mudarPagina(pagina() - 1)" [disabled]="pagina() === 1">← Anterior</button>
          @for (p of numerosPagina(); track p) {
            <button type="button"
                    [class.ativa]="p === pagina()"
                    (click)="mudarPagina(p)">
              {{ p }}
            </button>
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
      .acoes { white-space: nowrap; }
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
export class CandidatosComponent {
  private svc = inject(PartidoService);
  private router = inject(Router);

  candidatos = signal<Candidato[]>([]);
  processando = signal<number | null>(null);
  mensagem = signal<string | null>(null);
  erroMsg = signal(false);
  pagina = signal(1);
  private readonly TAMANHO_PAGINA = 10;

  cargos = CARGOS;
  ufs = UFS;
  filtros: Filtros = { nome: '', cargo: '', uf: '', municipio: '', status: 'ativos' };

  filtrados = computed<Candidato[]>(() => {
    const f = this.filtros;
    return this.candidatos().filter(c => {
      if (f.status === 'ativos' && !c.ativo) return false;
      if (f.status === 'inativos' && c.ativo) return false;
      if (f.nome && !c.nomeCompleto.toLowerCase().includes(f.nome.toLowerCase())) return false;
      if (f.cargo && c.cargo !== f.cargo) return false;
      if (f.uf && c.uf !== f.uf) return false;
      if (f.municipio && !(c.municipio ?? '').toLowerCase().includes(f.municipio.toLowerCase())) return false;
      return true;
    });
  });

  totalPaginas = computed(() => Math.max(1, Math.ceil(this.filtrados().length / this.TAMANHO_PAGINA)));

  paginaAtualItens = computed(() => {
    const inicio = (this.pagina() - 1) * this.TAMANHO_PAGINA;
    return this.filtrados().slice(inicio, inicio + this.TAMANHO_PAGINA);
  });

  numerosPagina = computed(() => {
    const total = this.totalPaginas();
    const atual = this.pagina();
    const janela = 5;
    const inicio = Math.max(1, atual - Math.floor(janela / 2));
    const fim = Math.min(total, inicio + janela - 1);
    const nums: number[] = [];
    for (let i = inicio; i <= fim; i++) nums.push(i);
    return nums;
  });

  constructor() {
    this.recarregar();
  }

  recarregar(): void {
    this.svc.listarCandidatos().subscribe({
      next: cs => this.candidatos.set(cs),
      error: () => this.mostrarErro('Falha ao carregar candidatos.'),
    });
  }

  rotuloCargo(c: Cargo): string {
    return CARGOS.find(x => x.valor === c)?.rotulo ?? c;
  }

  resetPagina(): void {
    this.pagina.set(1);
  }

  mudarPagina(p: number): void {
    if (p < 1 || p > this.totalPaginas()) return;
    this.pagina.set(p);
  }

  limparFiltros(): void {
    this.filtros = { nome: '', cargo: '', uf: '', municipio: '', status: 'ativos' };
    this.resetPagina();
  }

  async inativar(c: Candidato): Promise<void> {
    if (!confirm(`Inativar o candidato "${c.nomeCompleto}"?\nEle permanece no banco mas sai da listagem padrão.`)) return;
    this.processando.set(c.id);
    try {
      const atualizado = await firstValueFrom(this.svc.inativarCandidato(c.id));
      this.substituir(atualizado);
      this.mostrarOk(`Candidato "${c.nomeCompleto}" inativado.`);
    } catch (e) {
      this.mostrarErro('Falha ao inativar: ' + (this.extrairMensagem(e) ?? 'erro inesperado'));
    } finally {
      this.processando.set(null);
    }
  }

  async reativar(c: Candidato): Promise<void> {
    this.processando.set(c.id);
    try {
      const atualizado = await firstValueFrom(this.svc.reativarCandidato(c.id));
      this.substituir(atualizado);
      this.mostrarOk(`Candidato "${c.nomeCompleto}" reativado.`);
    } catch (e) {
      this.mostrarErro('Falha ao reativar: ' + (this.extrairMensagem(e) ?? 'erro inesperado'));
    } finally {
      this.processando.set(null);
    }
  }

  private substituir(atualizado: Candidato): void {
    this.candidatos.update(list => list.map(c => (c.id === atualizado.id ? atualizado : c)));
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
