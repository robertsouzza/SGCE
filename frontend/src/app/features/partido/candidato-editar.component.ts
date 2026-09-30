import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Cargo, CARGOS, PartidoService, UFS } from './partido.service';

@Component({
  selector: 'sgce-candidato-editar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <a routerLink="/candidatos" class="voltar">← Voltar para candidatos</a>
      <h1>Editar candidato</h1>

      @if (carregando()) {
        <p>Carregando…</p>
      } @else if (naoEncontrado()) {
        <div class="error card">Candidato não encontrado.</div>
      } @else {
        <form class="card" (ngSubmit)="salvar()">
          <label>
            Nome completo *
            <input name="nome" [(ngModel)]="form.nomeCompleto" required />
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
              {{ salvando() ? 'Salvando…' : 'Salvar alterações' }}
            </button>
          </div>
          @if (erroValidacao()) { <div class="error">{{ erroValidacao() }}</div> }
          @if (erro()) { <div class="error">{{ erro() }}</div> }
        </form>
      }
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
export class CandidatoEditarComponent implements OnInit {
  private svc = inject(PartidoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  form = {
    nomeCompleto: '',
    tituloEleitor: '',
    numeroCandidato: 10,
    cargo: 'SENADOR' as Cargo,
    uf: 'SP',
    municipio: '',
  };
  salvando = signal(false);
  carregando = signal(true);
  naoEncontrado = signal(false);
  erro = signal<string | null>(null);
  private id = 0;

  cargos = CARGOS;
  ufs = UFS;

  exigeMunicipio = computed(() => PartidoService.exigeMunicipio(this.form.cargo));
  erroValidacao = computed(() =>
    this.exigeMunicipio() && !this.form.municipio.trim()
      ? 'Município é obrigatório para cargos PREFEITO/VEREADOR.'
      : null,
  );

  async ngOnInit(): Promise<void> {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.naoEncontrado.set(true);
      this.carregando.set(false);
      return;
    }
    try {
      const c = await firstValueFrom(this.svc.buscarCandidato(this.id));
      this.form = {
        nomeCompleto: c.nomeCompleto,
        tituloEleitor: c.tituloEleitor,
        numeroCandidato: c.numeroCandidato,
        cargo: c.cargo,
        uf: c.uf,
        municipio: c.municipio ?? '',
      };
    } catch {
      this.naoEncontrado.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(): Promise<void> {
    if (this.erroValidacao()) return;
    this.erro.set(null);
    this.salvando.set(true);
    try {
      await firstValueFrom(
        this.svc.atualizarCandidato(this.id, {
          ...this.form,
          municipio: this.exigeMunicipio() ? this.form.municipio : undefined,
        }),
      );
      this.router.navigate(['/candidatos']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao atualizar candidato.');
    } finally {
      this.salvando.set(false);
    }
  }
}
