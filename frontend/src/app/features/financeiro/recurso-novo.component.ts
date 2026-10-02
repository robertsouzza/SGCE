import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { FinanceiroService, RecursoInput } from './financeiro.service';
import { RecursoFormComponent } from './recurso-form.component';

@Component({
  selector: 'sgce-recurso-novo',
  standalone: true,
  imports: [CommonModule, RouterLink, RecursoFormComponent],
  template: `
    <div class="page">
      <a routerLink="/financeiro" class="voltar">← Voltar para financeiro</a>
      <h1>Novo recurso</h1>
      <sgce-recurso-form
        textoSalvar="Cadastrar"
        [salvando]="salvando()"
        [erro]="erro()"
        (submeter)="criar($event)" />
    </div>
  `,
  styles: [
    `.page { max-width: 640px; } .voltar { color: #475569; text-decoration: none; font-size: 14px; } .voltar:hover { text-decoration: underline; } h1 { margin: 8px 0 16px; }`,
  ],
})
export class RecursoNovoComponent {
  private svc = inject(FinanceiroService);
  private router = inject(Router);

  salvando = signal(false);
  erro = signal<string | null>(null);

  criar(payload: RecursoInput): void {
    this.erro.set(null);
    this.salvando.set(true);
    this.svc.criarRecurso(payload).subscribe({
      next: () => this.router.navigate(['/financeiro']),
      error: err => {
        this.salvando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha ao criar recurso.');
      },
    });
  }
}
