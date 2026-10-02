import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Recurso, RecursoInput, TipoRecurso } from './financeiro.service';

@Component({
  selector: 'sgce-recurso-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <form class="card" (ngSubmit)="submeter.emit(form)">
      <label>
        Candidato (ID) *
        <input name="candidatoId" type="number" [(ngModel)]="form.candidatoId" required min="1" />
      </label>
      <label>
        Tipo de recurso *
        <select name="tipo" [(ngModel)]="form.tipoRecurso" required>
          <option value="FUNDO_ELEITORAL">Fundo Eleitoral</option>
          <option value="FUNDO_PARTIDARIO">Fundo Partidário</option>
          <option value="DOACAO">Doação</option>
        </select>
      </label>
      <label>
        Valor (R$) *
        <input name="valor" type="number" step="0.01" [(ngModel)]="form.valor" required min="0.01" />
      </label>
      <label>
        Data do repasse *
        <input name="dataRepasse" type="date" [(ngModel)]="form.dataRepasse" required />
      </label>
      <label>
        Origem
        <input name="origem" [(ngModel)]="form.origem" placeholder="Ex: TSE, CNPJ doador" />
      </label>
      <label>
        Nº do documento
        <input name="numeroDocumento" [(ngModel)]="form.numeroDocumento" />
      </label>
      <div class="acoes">
        <a routerLink="/financeiro" class="btn ghost">Cancelar</a>
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
export class RecursoFormComponent {
  @Input() textoSalvar = 'Cadastrar';
  @Input() salvando = false;
  @Input() erro: string | null = null;
  @Input() set recurso(r: Recurso | null) {
    if (r) {
      this.form = {
        candidatoId: r.candidatoId,
        tipoRecurso: r.tipoRecurso,
        valor: r.valor,
        dataRepasse: r.dataRepasse,
        origem: r.origem ?? '',
        numeroDocumento: r.numeroDocumento ?? '',
      };
    }
  }
  @Output() submeter = new EventEmitter<RecursoInput>();

  form: RecursoInput = {
    candidatoId: 0,
    tipoRecurso: 'FUNDO_ELEITORAL' as TipoRecurso,
    valor: 0,
    dataRepasse: new Date().toISOString().substring(0, 10),
    origem: '',
    numeroDocumento: '',
  };
}
