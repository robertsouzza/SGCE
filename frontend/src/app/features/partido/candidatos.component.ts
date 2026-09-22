import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { Cargo, Candidato, CARGOS, PartidoService, UFS } from './partido.service';

@Component({
  selector: 'sgce-candidatos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1>Candidatos</h1>

    <form class="card" (ngSubmit)="criar()">
      <h2>Novo candidato</h2>
      <label>Nome completo <input name="nome" [(ngModel)]="form.nomeCompleto" required /></label>
      <label>Título de eleitor <input name="titulo" [(ngModel)]="form.tituloEleitor" required /></label>
      <label>Número (candidato) <input name="numero" type="number" [(ngModel)]="form.numeroCandidato" required /></label>
      <label>Cargo
        <select name="cargo" [(ngModel)]="form.cargo" required>
          @for (c of cargos; track c.valor) {
            <option [value]="c.valor">{{ c.rotulo }}</option>
          }
        </select>
      </label>
      <label>UF
        <select name="uf" [(ngModel)]="form.uf" required>
          @for (uf of ufs; track uf.sigla) {
            <option [value]="uf.sigla">{{ uf.sigla }} — {{ uf.nome }}</option>
          }
        </select>
      </label>
      @if (exigeMunicipio()) {
        <label>Município <input name="municipio" [(ngModel)]="form.municipio" required /></label>
      }
      <button type="submit" [disabled]="salvando()">Cadastrar</button>
      @if (erroValidacao()) { <div class="error">{{ erroValidacao() }}</div> }
      @if (erro()) { <div class="error">{{ erro() }}</div> }
    </form>

    <h2 style="margin-top: 32px;">Candidatos</h2>
    <table>
      <thead><tr><th>ID</th><th>Nome</th><th>Cargo</th><th>UF</th><th>Município</th></tr></thead>
      <tbody>
        @for (c of candidatos(); track c.id) {
          <tr><td>{{ c.id }}</td><td>{{ c.nomeCompleto }}</td><td>{{ c.cargo }}</td><td>{{ c.uf }}</td><td>{{ c.municipio || '—' }}</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class CandidatosComponent {
  private svc = inject(PartidoService);
  private auth = inject(AuthService);

  form = {
    partidoId: 0,
    nomeCompleto: '',
    tituloEleitor: '',
    numeroCandidato: 10,
    cargo: 'SENADOR' as Cargo,
    uf: 'SP',
    municipio: '',
  };
  candidatos = signal<Candidato[]>([]);
  salvando = signal(false);
  erro = signal<string | null>(null);
  cargos = CARGOS;
  ufs = UFS;

  exigeMunicipio = computed(() => PartidoService.exigeMunicipio(this.form.cargo));
  erroValidacao = computed(() =>
    this.exigeMunicipio() && !this.form.municipio.trim()
      ? 'Município é obrigatório para cargos PREFEITO/VEREADOR.'
      : null,
  );

  constructor() { this.recarregar(); }

  recarregar(): void {
    this.svc.listarCandidatos().subscribe(cs => this.candidatos.set(cs));
  }

  criar(): void {
    if (this.erroValidacao()) return;
    this.erro.set(null);
    this.salvando.set(true);

    const u = this.auth.user();
    const partidoId = this.form.partidoId || (typeof u?.partidoId === 'number' ? u.partidoId : 0);

    this.svc
      .criarCandidato({
        ...this.form,
        partidoId,
        municipio: this.exigeMunicipio() ? this.form.municipio : undefined,
      })
      .subscribe({
        next: () => {
          this.form.nomeCompleto = '';
          this.form.tituloEleitor = '';
          this.form.municipio = '';
          this.salvando.set(false);
          this.recarregar();
        },
        error: err => {
          this.salvando.set(false);
          this.erro.set(err?.error?.mensagem ?? 'Falha ao criar candidato.');
        },
      });
  }
}
