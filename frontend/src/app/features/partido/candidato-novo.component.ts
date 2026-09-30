import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Cargo, CARGOS, PartidoService, UFS } from './partido.service';

@Component({
  selector: 'sgce-candidato-novo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <a routerLink="/candidatos" class="voltar">← Voltar para candidatos</a>
      <h1>Novo candidato</h1>

      <form class="card" (ngSubmit)="criar()">
        <label>
          Nome completo *
          <input name="nome" [(ngModel)]="form.nomeCompleto" required autofocus />
        </label>
        <label>
          Título de eleitor *
          <input name="titulo" [(ngModel)]="form.tituloEleitor" required />
        </label>
        <label>
          Número (candidato) *
          <input name="numero" type="number" [(ngModel)]="form.numeroCandidato" required min="10" />
        </label>
        <label>
          Cargo *
          <select name="cargo" [(ngModel)]="form.cargo" required>
            @for (c of cargos; track c.valor) {
              <option [value]="c.valor">{{ c.rotulo }}</option>
            }
          </select>
        </label>
        <label>
          UF *
          <select name="uf" [(ngModel)]="form.uf" required>
            @for (u of ufs; track u.sigla) {
              <option [value]="u.sigla">{{ u.sigla }} — {{ u.nome }}</option>
            }
          </select>
        </label>
        @if (exigeMunicipio()) {
          <label>
            Município * (obrigatório para PREFEITO/VEREADOR)
            <input name="municipio" [(ngModel)]="form.municipio" required />
          </label>
        }
        <div class="acoes">
          <a routerLink="/candidatos" class="btn ghost">Cancelar</a>
          <button type="submit" class="btn primary" [disabled]="salvando() || !!erroValidacao()">
            {{ salvando() ? 'Salvando…' : 'Cadastrar' }}
          </button>
        </div>
        @if (erroValidacao()) { <div class="error">{{ erroValidacao() }}</div> }
        @if (erro()) { <div class="error">{{ erro() }}</div> }
      </form>
    </div>
  `,
  styles: [
    `
      .page { max-width: 640px; }
      .voltar { color: #475569; text-decoration: none; font-size: 14px; }
      .voltar:hover { text-decoration: underline; }
      h1 { margin: 8px 0 16px; }
      .card { background: #fff; padding: 20px; border-radius: 6px; }
      form label { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; font-size: 14px; color: #334155; }
      form input, form select { padding: 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
      .btn { padding: 8px 16px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; text-decoration: none; color: #1e293b; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn.ghost { background: transparent; color: #475569; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .error { color: #dc2626; margin-top: 12px; font-size: 14px; }
    `,
  ],
})
export class CandidatoNovoComponent {
  private svc = inject(PartidoService);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = {
    partidoId: 0,
    nomeCompleto: '',
    tituloEleitor: '',
    numeroCandidato: 10,
    cargo: 'SENADOR' as Cargo,
    uf: 'SP',
    municipio: '',
  };
  salvando = signal(false);
  erro = signal<string | null>(null);

  cargos = CARGOS;
  ufs = UFS;

  exigeMunicipio = computed(() => PartidoService.exigeMunicipio(this.form.cargo));
  erroValidacao = computed(() =>
    this.exigeMunicipio() && !this.form.municipio.trim()
      ? 'Município é obrigatório para cargos PREFEITO/VEREADOR.'
      : null,
  );

  criar(): void {
    if (this.erroValidacao()) return;
    this.erro.set(null);
    this.salvando.set(true);

    const u = this.auth.user();
    const partidoId = this.form.partidoId || (typeof u?.partidoId === 'number' ? u.partidoId : 0);

    this.svc
      .criarCandidato({
        ...this.form,
        partidoId,
        municipio: this.exigeMunicipio() ? this.form.municipio : undefined,
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/candidatos']);
        },
        error: err => {
          this.salvando.set(false);
          this.erro.set(err?.error?.mensagem ?? 'Falha ao criar candidato.');
        },
      });
  }
}
