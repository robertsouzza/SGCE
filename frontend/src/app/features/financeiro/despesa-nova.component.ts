import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CategoriaDespesa, FinanceiroService } from './financeiro.service';

@Component({
  selector: 'sgce-despesa-nova',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <a routerLink="/financeiro" class="voltar">← Voltar para financeiro</a>
      <h1>Lançar despesa</h1>

      <form class="card" (ngSubmit)="lancar()">
        <label>
          Candidato (ID) *
          <input name="candidatoId" type="number" [(ngModel)]="form.candidatoId" required min="1" />
        </label>
        <label>
          Categoria *
          <select name="categoria" [(ngModel)]="form.categoria" required>
            <option value="PESSOAL">Pessoal</option>
            <option value="ALIMENTACAO">Alimentação</option>
            <option value="TRANSPORTE">Transporte</option>
            <option value="MATERIAL_GRAFICO">Material gráfico</option>
            <option value="OUTROS">Outros</option>
          </select>
        </label>
        <label>
          Valor (R$) *
          <input name="valor" type="number" step="0.01" [(ngModel)]="form.valor" required min="0.01" />
        </label>
        <label>
          Data *
          <input name="data" type="date" [(ngModel)]="form.data" required />
        </label>
        <label>
          Descrição
          <input name="descricao" [(ngModel)]="form.descricao" />
        </label>
        <label>
          Comprovante (PDF, imagem — opcional)
          <input name="arquivo" type="file" (change)="onArquivo($event)" accept="application/pdf,image/*" />
        </label>
        <div class="acoes">
          <a routerLink="/financeiro" class="btn ghost">Cancelar</a>
          <button type="submit" class="btn primary" [disabled]="salvando()">
            {{ salvando() ? 'Lançando…' : 'Lançar despesa' }}
          </button>
        </div>
        @if (erro()) { <div class="error">{{ erro() }}</div> }
      </form>
    </div>
  `,
  styles: [
    `
      .page { max-width: 640px; }
      .voltar { color: #475569; text-decoration: none; font-size: 14px; }
      .voltar:hover { text-decoration: underline; }
      h1 { margin: 8px 0 16px; }
      .card { background: #fff; padding: 20px; border-radius: 6px; }
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
export class DespesaNovaComponent {
  private svc = inject(FinanceiroService);
  private router = inject(Router);

  form = {
    candidatoId: 0,
    categoria: 'OUTROS' as CategoriaDespesa,
    valor: 0,
    data: new Date().toISOString().substring(0, 10),
    descricao: '',
  };
  arquivoComprovante: File | null = null;
  salvando = signal(false);
  erro = signal<string | null>(null);

  onArquivo(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.arquivoComprovante = input.files?.[0] ?? null;
  }

  async lancar(): Promise<void> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      const d = await firstValueFrom(this.svc.criarDespesa(this.form));
      if (this.arquivoComprovante) {
        await firstValueFrom(this.svc.anexarComprovante(d.id, this.arquivoComprovante));
      }
      this.router.navigate(['/financeiro']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao lançar despesa.');
    } finally {
      this.salvando.set(false);
    }
  }
}
