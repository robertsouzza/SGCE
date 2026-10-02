import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { CriarPartidoInput, PartidoService } from './partido.service';
import { PartidoFormComponent } from './partido-form.component';

@Component({
  selector: 'sgce-partido-novo',
  standalone: true,
  imports: [CommonModule, RouterLink, PartidoFormComponent],
  template: `
    <div class="page">
      <a routerLink="/partidos" class="voltar">← Voltar para partidos</a>
      <h1>Novo partido</h1>
      <sgce-partido-form
        textoSalvar="Cadastrar"
        [salvando]="salvando()"
        [erro]="erro()"
        (submeter)="criar($event)" />
    </div>
  `,
  styles: [
    `
      .page { max-width: 640px; }
      .voltar { color: #475569; text-decoration: none; font-size: 14px; }
      .voltar:hover { text-decoration: underline; }
      h1 { margin: 8px 0 16px; }
    `,
  ],
})
export class PartidoNovoComponent {
  private svc = inject(PartidoService);
  private router = inject(Router);

  salvando = signal(false);
  erro = signal<string | null>(null);

  criar(payload: CriarPartidoInput): void {
    this.erro.set(null);
    this.salvando.set(true);
    this.svc.criar(payload).subscribe({
      next: () => this.router.navigate(['/partidos']),
      error: err => {
        this.salvando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha ao criar partido.');
      },
    });
  }
}
