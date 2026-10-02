import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CriarPartidoInput, Partido } from './partido.service';

/**
 * Form reutilizável para criar e editar partido.
 * Emite (submeter) com o payload quando o usuário salva.
 */
@Component({
  selector: 'sgce-partido-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <form class="card" (ngSubmit)="submeter.emit(form)">
      <label>
        Nome *
        <input name="nome" [(ngModel)]="form.nome" required />
      </label>
      <label>
        Sigla *
        <input name="sigla" [(ngModel)]="form.sigla" maxlength="20" required />
      </label>
      <label>
        Número do partido (10–99) *
        <input name="numeroPartido" type="number" min="10" max="99"
               [(ngModel)]="form.numeroPartido" required />
      </label>
      <label>
        CNPJ *
        <input name="cnpj" [(ngModel)]="form.cnpj" required />
      </label>
      <label>
        E-mail
        <input name="email" type="email" [(ngModel)]="form.email" />
      </label>
      <label>
        Telefone (formato E.164, ex: +5511999999999)
        <input name="telefone" [(ngModel)]="form.telefone" placeholder="+5511999999999" />
      </label>
      <label>
        Endereço da sede
        <input name="enderecoSede" [(ngModel)]="form.enderecoSede" />
      </label>
      <label>
        Dados bancários (conta partidária)
        <input name="dadosBancarios" [(ngModel)]="form.dadosBancariosContaPartidaria" />
      </label>
      <label>
        Plano de assinatura
        <select name="plano" [(ngModel)]="form.planoAssinatura">
          <option value="FREE">Free</option>
          <option value="BASIC">Basic</option>
          <option value="PREMIUM">Premium</option>
        </select>
      </label>
      <div class="acoes">
        <a routerLink="/partidos" class="btn ghost">Cancelar</a>
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
export class PartidoFormComponent {
  @Input() textoSalvar = 'Cadastrar';
  @Input() salvando = false;
  @Input() erro: string | null = null;
  @Input() set partido(p: Partido | null) {
    if (p) {
      this.form = {
        nome: p.nome,
        sigla: p.sigla,
        numeroPartido: p.numeroPartido,
        cnpj: p.cnpj,
        enderecoSede: '',
        dadosBancariosContaPartidaria: '',
        email: '',
        telefone: '',
        planoAssinatura: p.planoAssinatura,
      };
    }
  }
  @Output() submeter = new EventEmitter<CriarPartidoInput>();

  form: CriarPartidoInput = {
    nome: '',
    sigla: '',
    numeroPartido: 10,
    cnpj: '',
    enderecoSede: '',
    dadosBancariosContaPartidaria: '',
    email: '',
    telefone: '',
    planoAssinatura: 'FREE',
  };
}
