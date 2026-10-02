import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CriarPartidoInput, Partido, PartidoService } from './partido.service';
import { PartidoFormComponent } from './partido-form.component';

@Component({
  selector: 'sgce-partido-editar',
  standalone: true,
  imports: [CommonModule, RouterLink, PartidoFormComponent],
  template: `
    <div class="page">
      <a routerLink="/partidos" class="voltar">← Voltar para partidos</a>
      <h1>Editar partido</h1>

      @if (carregando()) {
        <p>Carregando…</p>
      } @else if (naoEncontrado()) {
        <div class="error card">Partido não encontrado.</div>
      } @else {
        <sgce-partido-form
          textoSalvar="Salvar alterações"
          [partido]="partido()"
          [salvando]="salvando()"
          [erro]="erro()"
          (submeter)="salvar($event)" />
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 640px; }
      .voltar { color: #475569; text-decoration: none; font-size: 14px; }
      .voltar:hover { text-decoration: underline; }
      h1 { margin: 8px 0 16px; }
      .error.card { background: #fee2e2; color: #991b1b; padding: 16px; border-radius: 6px; }
    `,
  ],
})
export class PartidoEditarComponent implements OnInit {
  private svc = inject(PartidoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  partido = signal<Partido | null>(null);
  carregando = signal(true);
  naoEncontrado = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);
  private id = 0;

  async ngOnInit(): Promise<void> {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.naoEncontrado.set(true);
      this.carregando.set(false);
      return;
    }
    try {
      const p = await firstValueFrom(this.svc.buscar(this.id));
      this.partido.set(p);
    } catch {
      this.naoEncontrado.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(payload: CriarPartidoInput): Promise<void> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      await firstValueFrom(this.svc.atualizar(this.id, payload));
      this.router.navigate(['/partidos']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao atualizar partido.');
    } finally {
      this.salvando.set(false);
    }
  }
}
