import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Equipe, EquipeService } from './equipe.service';

@Component({
  selector: 'sgce-equipes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Equipes</h1>

    <form class="card" (ngSubmit)="criar()">
      <h2>Nova equipe</h2>
      <label>Nome <input name="nome" [(ngModel)]="form.nome" required /></label>
      <label>Líder (usuário ID) <input name="liderId" type="number" [(ngModel)]="form.liderId" required /></label>
      <label>Região de atuação <input name="regiaoAtuacao" [(ngModel)]="form.regiaoAtuacao" /></label>
      <button type="submit" [disabled]="salvando()">Cadastrar</button>
      @if (erro()) { <div class="error">{{ erro() }}</div> }
    </form>

    <h2 style="margin-top: 32px;">Equipes cadastradas</h2>
    <table>
      <thead><tr><th>ID</th><th>Nome</th><th>Líder</th><th>Região</th><th>Ações</th></tr></thead>
      <tbody>
        @for (e of equipes(); track e.id) {
          <tr>
            <td>{{ e.id }}</td><td>{{ e.nome }}</td><td>{{ e.liderId }}</td><td>{{ e.regiaoAtuacao || '—' }}</td>
            <td>
              <button (click)="prepararMembro(e.id)">+ Membro</button>
              <button (click)="prepararVinculoCandidato(e.id)">+ Candidato</button>
            </td>
          </tr>
        }
      </tbody>
    </table>

    @if (equipeAlvoMembro()) {
      <form class="card" style="margin-top: 24px;" (ngSubmit)="salvarMembro()">
        <h3>Adicionar membro à equipe {{ equipeAlvoMembro() }}</h3>
        <label>Usuário ID <input name="usuarioId" type="number" [(ngModel)]="formMembro.usuarioId" required /></label>
        <label>Função <input name="funcao" [(ngModel)]="formMembro.funcao" /></label>
        <button type="submit">Adicionar</button>
        <button type="button" (click)="equipeAlvoMembro.set(null)">Cancelar</button>
      </form>
    }

    @if (equipeAlvoVinculo()) {
      <form class="card" style="margin-top: 24px;" (ngSubmit)="salvarVinculo()">
        <h3>Vincular candidato à equipe {{ equipeAlvoVinculo() }}</h3>
        <label>Candidato ID <input name="candidatoId" type="number" [(ngModel)]="formVinculo.candidatoId" required /></label>
        <label>Vigente desde <input name="vigenteDesde" type="date" [(ngModel)]="formVinculo.vigenteDesde" required /></label>
        <button type="submit">Vincular</button>
        <button type="button" (click)="equipeAlvoVinculo.set(null)">Cancelar</button>
      </form>
    }
  `,
})
export class EquipesComponent {
  private svc = inject(EquipeService);

  form = { nome: '', liderId: 0, regiaoAtuacao: '' };
  equipes = signal<Equipe[]>([]);
  salvando = signal(false);
  erro = signal<string | null>(null);

  equipeAlvoMembro = signal<number | null>(null);
  formMembro = { usuarioId: 0, funcao: '' };

  equipeAlvoVinculo = signal<number | null>(null);
  formVinculo = { candidatoId: 0, vigenteDesde: new Date().toISOString().substring(0, 10) };

  constructor() { this.recarregar(); }

  recarregar(): void {
    this.svc.listar().subscribe(es => this.equipes.set(es));
  }

  criar(): void {
    this.erro.set(null);
    this.salvando.set(true);
    this.svc.criar({ nome: this.form.nome, liderId: this.form.liderId, regiaoAtuacao: this.form.regiaoAtuacao || undefined }).subscribe({
      next: () => {
        this.form = { nome: '', liderId: 0, regiaoAtuacao: '' };
        this.salvando.set(false);
        this.recarregar();
      },
      error: err => {
        this.salvando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha ao criar equipe.');
      },
    });
  }

  prepararMembro(equipeId: number): void { this.equipeAlvoMembro.set(equipeId); }
  salvarMembro(): void {
    const id = this.equipeAlvoMembro();
    if (!id) return;
    this.svc.adicionarMembro(id, this.formMembro).subscribe(() => {
      this.formMembro = { usuarioId: 0, funcao: '' };
      this.equipeAlvoMembro.set(null);
    });
  }

  prepararVinculoCandidato(equipeId: number): void { this.equipeAlvoVinculo.set(equipeId); }
  salvarVinculo(): void {
    const id = this.equipeAlvoVinculo();
    if (!id) return;
    this.svc.vincularCandidato(id, this.formVinculo).subscribe(() => {
      this.formVinculo = { candidatoId: 0, vigenteDesde: new Date().toISOString().substring(0, 10) };
      this.equipeAlvoVinculo.set(null);
    });
  }
}
