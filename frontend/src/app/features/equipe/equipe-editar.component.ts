import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AtualizarEquipeInput, Equipe, EquipeCandidato, EquipeService, MembroEquipe } from './equipe.service';
import { EquipeFormComponent } from './equipe-form.component';

@Component({
  selector: 'sgce-equipe-editar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, EquipeFormComponent],
  template: `
    <div class="page">
      <a routerLink="/equipes" class="voltar">← Voltar para equipes</a>
      <h1>Editar equipe</h1>

      @if (carregando()) {
        <p>Carregando…</p>
      } @else if (naoEncontrada()) {
        <div class="error card">Equipe não encontrada.</div>
      } @else {
        <sgce-equipe-form
          textoSalvar="Salvar alterações"
          [equipe]="equipe()"
          [salvando]="salvando()"
          [erro]="erro()"
          (submeter)="salvar($event)" />

        <section class="card secao" style="margin-top: 24px;">
          <h2>Membros da equipe</h2>
          <ul class="itens">
            @for (m of membros(); track m.id) {
              <li>Usuário #{{ m.usuarioId }} <span class="meta">{{ m.funcao || '—' }}</span></li>
            } @empty {
              <li class="vazio">Nenhum membro cadastrado.</li>
            }
          </ul>
          <div class="sub-form">
            <input placeholder="ID do usuário" type="number" [(ngModel)]="formMembro.usuarioId" name="usuarioId" />
            <input placeholder="Função (opcional)" [(ngModel)]="formMembro.funcao" name="funcao" />
            <button type="button" class="btn primary" (click)="adicionarMembro()" [disabled]="salvandoMembro()">
              {{ salvandoMembro() ? 'Adicionando…' : '+ Adicionar membro' }}
            </button>
          </div>
          @if (erroMembro()) { <div class="error">{{ erroMembro() }}</div> }
        </section>

        <section class="card secao" style="margin-top: 16px;">
          <h2>Candidatos vinculados</h2>
          <ul class="itens">
            @for (v of vinculos(); track v.id) {
              <li>Candidato #{{ v.candidatoId }} <span class="meta">desde {{ v.vigenteDesde }}{{ v.vigenteAte ? ' até ' + v.vigenteAte : '' }}</span></li>
            } @empty {
              <li class="vazio">Nenhum candidato vinculado.</li>
            }
          </ul>
          <div class="sub-form">
            <input placeholder="ID do candidato" type="number" [(ngModel)]="formVinculo.candidatoId" name="candidatoId" />
            <input type="date" [(ngModel)]="formVinculo.vigenteDesde" name="vigenteDesde" />
            <button type="button" class="btn primary" (click)="vincularCandidato()" [disabled]="salvandoVinculo()">
              {{ salvandoVinculo() ? 'Vinculando…' : '+ Vincular candidato' }}
            </button>
          </div>
          @if (erroVinculo()) { <div class="error">{{ erroVinculo() }}</div> }
        </section>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 720px; }
      .voltar { color: #475569; text-decoration: none; font-size: 14px; }
      .voltar:hover { text-decoration: underline; }
      h1 { margin: 8px 0 16px; }
      h2 { margin: 0 0 12px; font-size: 16px; }
      .card { background: #fff; padding: 20px; border-radius: 6px; }
      .error.card { background: #fee2e2; color: #991b1b; padding: 16px; border-radius: 6px; }
      .secao .itens { list-style: none; padding: 0; margin: 0 0 12px; }
      .secao .itens li { padding: 6px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
      .secao .itens li.vazio { color: #94a3b8; font-style: italic; border-bottom: none; }
      .meta { color: #64748b; font-size: 12px; margin-left: 8px; }
      .sub-form { display: flex; gap: 8px; align-items: center; }
      .sub-form input { flex: 1; padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .btn { padding: 6px 12px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; font-size: 13px; }
      .btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      .btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .error { color: #dc2626; margin-top: 8px; font-size: 13px; }
    `,
  ],
})
export class EquipeEditarComponent implements OnInit {
  private svc = inject(EquipeService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  equipe = signal<Equipe | null>(null);
  carregando = signal(true);
  naoEncontrada = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);

  membros = signal<MembroEquipe[]>([]);
  vinculos = signal<EquipeCandidato[]>([]);
  formMembro = { usuarioId: 0, funcao: '' };
  formVinculo = { candidatoId: 0, vigenteDesde: new Date().toISOString().substring(0, 10) };
  salvandoMembro = signal(false);
  salvandoVinculo = signal(false);
  erroMembro = signal<string | null>(null);
  erroVinculo = signal<string | null>(null);

  private id = 0;

  async ngOnInit(): Promise<void> {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.naoEncontrada.set(true);
      this.carregando.set(false);
      return;
    }
    try {
      const [e, m, v] = await Promise.all([
        firstValueFrom(this.svc.buscar(this.id)),
        firstValueFrom(this.svc.listarMembros(this.id)),
        firstValueFrom(this.svc.listarCandidatosDaEquipe(this.id)),
      ]);
      this.equipe.set(e);
      this.membros.set(m);
      this.vinculos.set(v);
    } catch {
      this.naoEncontrada.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(payload: AtualizarEquipeInput): Promise<void> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      await firstValueFrom(
        this.svc.atualizar(this.id, {
          nome: payload.nome,
          liderId: payload.liderId,
          regiaoAtuacao: payload.regiaoAtuacao?.trim() || undefined,
        }),
      );
      this.router.navigate(['/equipes']);
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erro.set(e?.error?.mensagem ?? 'Falha ao atualizar equipe.');
    } finally {
      this.salvando.set(false);
    }
  }

  async adicionarMembro(): Promise<void> {
    if (!this.formMembro.usuarioId) {
      this.erroMembro.set('Informe o ID do usuário.');
      return;
    }
    this.erroMembro.set(null);
    this.salvandoMembro.set(true);
    try {
      const novo = await firstValueFrom(
        this.svc.adicionarMembro(this.id, {
          usuarioId: this.formMembro.usuarioId,
          funcao: this.formMembro.funcao?.trim() || undefined,
        }),
      );
      this.membros.update(list => [...list, novo]);
      this.formMembro = { usuarioId: 0, funcao: '' };
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erroMembro.set(e?.error?.mensagem ?? 'Falha ao adicionar membro.');
    } finally {
      this.salvandoMembro.set(false);
    }
  }

  async vincularCandidato(): Promise<void> {
    if (!this.formVinculo.candidatoId) {
      this.erroVinculo.set('Informe o ID do candidato.');
      return;
    }
    this.erroVinculo.set(null);
    this.salvandoVinculo.set(true);
    try {
      const novo = await firstValueFrom(
        this.svc.vincularCandidato(this.id, {
          candidatoId: this.formVinculo.candidatoId,
          vigenteDesde: this.formVinculo.vigenteDesde,
        }),
      );
      this.vinculos.update(list => [...list, novo]);
      this.formVinculo = { candidatoId: 0, vigenteDesde: new Date().toISOString().substring(0, 10) };
    } catch (err: unknown) {
      const e = err as { error?: { mensagem?: string } };
      this.erroVinculo.set(e?.error?.mensagem ?? 'Falha ao vincular candidato.');
    } finally {
      this.salvandoVinculo.set(false);
    }
  }
}
