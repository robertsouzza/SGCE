import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AtualizarEquipeInput, Equipe } from './equipe.service';

@Component({
  selector: 'sgce-equipe-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <form class="card" (ngSubmit)="submeter.emit(form)">
      <label>
        Nome *
        <input name="nome" [(ngModel)]="form.nome" required />
      </label>
      <label>
        Líder (usuário ID) *
        <input name="liderId" type="number" [(ngModel)]="form.liderId" required min="1" />
        <small>ID do usuário no sistema que será o líder da equipe.</small>
      </label>
      <label>
        Região de atuação
        <input name="regiao" [(ngModel)]="form.regiaoAtuacao" placeholder="Ex: Zona Sul de SP" />
      </label>
      <div class="acoes">
        <a routerLink="/equipes" class="btn ghost">Cancelar</a>
        <button type="submit" class="btn primary" [disabled]="salvando">
          {{ salvando ? 'Salvando…' : textoSalvar }}
        </button>
      </div>
      @if (erro) { <div class="error">{{ erro }}</div> }
    </form>
  `,
  styles: [
    `
      .card { background: #fff; padding: 20px; border-radius: 6px; max-width: 640px; }
      form label { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; font-size: 14px; color: #334155; }
      form input { padding: 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      form small { color: #64748b; font-size: 12px; }
      .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
      .btn { padding: 8px 16px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; text-decoration: none; color: #1e293b; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn.ghost { background: transparent; color: #475569; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .error { color: #dc2626; margin-top: 12px; font-size: 14px; }
    `,
  ],
})
export class EquipeFormComponent {
  @Input() textoSalvar = 'Cadastrar';
  @Input() salvando = false;
  @Input() erro: string | null = null;
  @Input() set equipe(e: Equipe | null) {
    if (e) {
      this.form = { nome: e.nome, liderId: e.liderId, regiaoAtuacao: e.regiaoAtuacao ?? '' };
    }
  }
  @Output() submeter = new EventEmitter<AtualizarEquipeInput>();

  form: AtualizarEquipeInput = { nome: '', liderId: 0, regiaoAtuacao: '' };
}
