import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { FinanceiroService, Recurso, RecursoInput } from './financeiro.service';
import { RecursoFormComponent } from './recurso-form.component';

@Component({
  selector: 'sgce-recurso-editar',
  standalone: true,
  imports: [CommonModule, RouterLink, RecursoFormComponent],
  template: `
    <div class="page">
      <a routerLink="/financeiro" class="voltar">← Voltar para financeiro</a>
      <h1>Editar recurso</h1>

      @if (carregando()) {
        <p>Carregando…</p>
      } @else if (naoEncontrado()) {
        <div class="error card">Recurso não encontrado.</div>
      } @else {
        <sgce-recurso-form
          textoSalvar="Salvar alterações"
          [recurso]="recurso()"
          [salvando]="salvando()"
          [erro]="erro()"
          (submeter)="salvar($event)" />
      }
    </div>
  `,
  styles: [
    `.page { max-width: 640px; } .voltar { color: #475569; text-decoration: none; font-size: 14px; } .voltar:hover { text-decoration: underline; } h1 { margin: 8px 0 16px; } .error.card { background: #fee2e2; color: #991b1b; padding: 16px; border-radius: 6px; }`,
  ],
})
export class RecursoEditarComponent implements OnInit {
  private svc = inject(FinanceiroService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  recurso = signal<Recurso | null>(null);
  carregando = signal(true);
  naoEncontrado = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);
  private id = 0;

  async ngOnInit(): Promise<void> {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) { this.naoEncontrado.set(true); this.carregando.set(false); return; }
    try {
      const r = await firstValueFrom(this.svc.buscarRecurso(this.id));
      this.recurso.set(r);
    } catch { this.naoEncontrado.set(true); }
    finally { this.carregando.set(false); }
  }

  async salvar(payload: RecursoInput): Promise<void> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      await firstValueFrom(this.svc.atualizarRecurso(this.id, payload));
      this.router.navigate(['/financeiro']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao atualizar recurso.');
    } finally { this.salvando.set(false); }
  }
}
