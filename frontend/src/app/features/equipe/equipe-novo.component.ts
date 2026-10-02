import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { AtualizarEquipeInput, EquipeService } from './equipe.service';
import { EquipeFormComponent } from './equipe-form.component';

@Component({
  selector: 'sgce-equipe-novo',
  standalone: true,
  imports: [CommonModule, RouterLink, EquipeFormComponent],
  template: `
    <div class="page">
      <a routerLink="/equipes" class="voltar">← Voltar para equipes</a>
      <h1>Nova equipe</h1>
      <sgce-equipe-form
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
export class EquipeNovoComponent {
  private svc = inject(EquipeService);
  private router = inject(Router);

  salvando = signal(false);
  erro = signal<string | null>(null);

  criar(payload: AtualizarEquipeInput): void {
    this.erro.set(null);
    this.salvando.set(true);
    this.svc.criar({
      nome: payload.nome,
      liderId: payload.liderId,
      regiaoAtuacao: payload.regiaoAtuacao?.trim() || undefined,
    }).subscribe({
      next: () => this.router.navigate(['/equipes']),
      error: err => {
        this.salvando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha ao criar equipe.');
      },
    });
  }
}
