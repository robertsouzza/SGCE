import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CadastrarEleitorPayload, Eleitor, EleitorService } from './eleitor.service';

@Component({
  selector: 'sgce-eleitor-editar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <a routerLink="/eleitores" class="voltar">← Voltar para eleitores</a>
      <h1>Editar eleitor</h1>

      @if (carregando()) {
        <p>Carregando…</p>
      } @else if (naoEncontrado()) {
        <div class="error card">Eleitor não encontrado.</div>
      } @else if (anonimizado()) {
        <div class="aviso card">
          Este eleitor foi anonimizado (D-02 LGPD) — seus dados pessoais não podem mais ser editados.
          Esta ação é irreversível por desenho.
        </div>
      } @else {
        <form class="card" (ngSubmit)="salvar()">
          <label>Nome completo *
            <input name="nome" [(ngModel)]="form.nomeCompleto" required />
          </label>
          <label>Título de eleitor *
            <input name="titulo" [(ngModel)]="form.tituloEleitor" required />
          </label>
          <label>WhatsApp
            <input name="whats" type="tel" [(ngModel)]="form.telefoneWhatsapp" placeholder="+5511999999999" />
          </label>
          <label>Endereço
            <input name="endereco" [(ngModel)]="form.endereco" />
          </label>
          <label>Zona eleitoral
            <input name="zona" [(ngModel)]="form.zonaEleitoral" />
          </label>
          <label>Seção eleitoral
            <input name="secao" [(ngModel)]="form.secaoEleitoral" />
          </label>
          <label>Observações
            <input name="obs" [(ngModel)]="form.observacoes" />
          </label>
          <div class="acoes">
            <a routerLink="/eleitores" class="btn ghost">Cancelar</a>
            <button type="submit" class="btn primary" [disabled]="salvando()">
              {{ salvando() ? 'Salvando…' : 'Salvar alterações' }}
            </button>
          </div>
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
      .error.card { background: #fee2e2; color: #991b1b; }
      .aviso.card { background: #fef3c7; color: #92400e; }
      form label { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; font-size: 14px; color: #334155; }
      form input { padding: 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
      .btn { padding: 8px 16px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; text-decoration: none; color: #1e293b; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn.ghost { background: transparent; color: #475569; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .error { color: #dc2626; margin-top: 12px; font-size: 14px; }
    `,
  ],
})
export class EleitorEditarComponent implements OnInit {
  private svc = inject(EleitorService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  carregando = signal(true);
  naoEncontrado = signal(false);
  anonimizado = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);
  private id = 0;

  form: CadastrarEleitorPayload = {
    nomeCompleto: '',
    tituloEleitor: '',
    telefoneWhatsapp: '',
    endereco: '',
    zonaEleitoral: '',
    secaoEleitoral: '',
    observacoes: '',
    geolocalizacao: null,
  };

  async ngOnInit(): Promise<void> {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) { this.naoEncontrado.set(true); this.carregando.set(false); return; }
    try {
      const e: Eleitor = await firstValueFrom(this.svc.buscar(this.id));
      if (e.anonimizado) {
        this.anonimizado.set(true);
      } else {
        this.form = {
          nomeCompleto: e.nomeCompleto,
          tituloEleitor: e.tituloEleitor,
          telefoneWhatsapp: e.telefoneWhatsapp ?? '',
          endereco: e.endereco ?? '',
          zonaEleitoral: e.zonaEleitoral ?? '',
          secaoEleitoral: e.secaoEleitoral ?? '',
          observacoes: e.observacoes ?? '',
          geolocalizacao: e.geolocalizacao,
        };
      }
    } catch {
      this.naoEncontrado.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(): Promise<void> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      await firstValueFrom(this.svc.atualizar(this.id, {
        ...this.form,
        telefoneWhatsapp: this.form.telefoneWhatsapp?.trim() || null,
        endereco: this.form.endereco?.trim() || null,
        zonaEleitoral: this.form.zonaEleitoral?.trim() || null,
        secaoEleitoral: this.form.secaoEleitoral?.trim() || null,
        observacoes: this.form.observacoes?.trim() || null,
      }));
      this.router.navigate(['/eleitores']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao atualizar eleitor.');
    } finally {
      this.salvando.set(false);
    }
  }
}
