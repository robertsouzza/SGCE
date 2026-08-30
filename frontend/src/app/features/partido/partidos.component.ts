import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Partido, PartidoService } from './partido.service';

@Component({
  selector: 'sgce-partidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Partidos</h1>
    <p>Cadastro de partidos (visível só para SUPER_ADMIN).</p>

    <form class="card" (ngSubmit)="criar()">
      <h2>Novo partido</h2>
      <label>Nome <input name="nome" [(ngModel)]="form.nome" required /></label>
      <label>Sigla <input name="sigla" [(ngModel)]="form.sigla" maxlength="20" required /></label>
      <label>Número (10–99) <input name="numeroPartido" type="number" min="10" max="99" [(ngModel)]="form.numeroPartido" required /></label>
      <label>CNPJ <input name="cnpj" [(ngModel)]="form.cnpj" required /></label>
      <label>Telefone (E.164) <input name="telefone" [(ngModel)]="form.telefone" placeholder="5511999999999" /></label>
      <button type="submit" [disabled]="salvando()">Cadastrar</button>
      @if (erro()) { <div class="error">{{ erro() }}</div> }
    </form>

    <h2 style="margin-top: 32px;">Partidos cadastrados</h2>
    @if (carregando()) { <p>Carregando…</p> }
    <table>
      <thead><tr><th>ID</th><th>Nome</th><th>Sigla</th><th>Nº</th><th>CNPJ</th></tr></thead>
      <tbody>
        @for (p of partidos(); track p.id) {
          <tr><td>{{ p.id }}</td><td>{{ p.nome }}</td><td>{{ p.sigla }}</td><td>{{ p.numeroPartido }}</td><td>{{ p.cnpj }}</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class PartidosComponent {
  private svc = inject(PartidoService);

  form = { nome: '', sigla: '', numeroPartido: 10, cnpj: '', telefone: '' };
  partidos = signal<Partido[]>([]);
  carregando = signal(true);
  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor() { this.recarregar(); }

  recarregar(): void {
    this.carregando.set(true);
    this.svc.listar().subscribe({
      next: ps => { this.partidos.set(ps); this.carregando.set(false); },
      error: () => this.carregando.set(false),
    });
  }

  criar(): void {
    this.erro.set(null);
    this.salvando.set(true);
    this.svc.criar({ ...this.form, telefone: this.form.telefone || undefined }).subscribe({
      next: () => {
        this.form = { nome: '', sigla: '', numeroPartido: 10, cnpj: '', telefone: '' };
        this.salvando.set(false);
        this.recarregar();
      },
      error: err => {
        this.salvando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha ao criar partido.');
      },
    });
  }
}
