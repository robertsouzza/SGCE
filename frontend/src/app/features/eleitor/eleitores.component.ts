import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { v4 as uuid } from 'uuid';

import { OfflineStore, OutboxItem, EleitorLocal } from '../../core/offline/offline.store';
import { SyncService } from '../../core/offline/sync.service';
import { GeolocationService } from '../../shared/geolocation/geolocation.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  CadastrarEleitorPayload,
  Eleitor,
  EleitorService,
} from './eleitor.service';

interface LinhaEleitor {
  origem: 'servidor' | 'local';
  id: number | string;
  nomeCompleto: string;
  tituloEleitor: string;
  telefoneWhatsapp: string | null;
  status?: OutboxItem['status'];
  serverId?: number;
  clientId?: string;
}

/**
 * Lista + cadastro offline-first de eleitores (RF-12, RF-13, RNF-01).
 * O submit grava sempre em IndexedDB primeiro (via OfflineStore) e enfileira
 * no outbox — o SyncService drena quando há conexão. UX mostra badges de
 * status por linha (pendente / sincronizado / erro).
 */
@Component({
  selector: 'sgce-eleitores',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <header class="page-header">
        <h2>Eleitores da minha região</h2>
        <div class="acoes-topo">
          @if (temPendencias()) {
            <button (click)="sincronizarAgora()">Sincronizar agora ({{ pendentesCount() }})</button>
          }
          <button class="primary" (click)="abrirNovo()">+ Novo eleitor</button>
        </div>
      </header>

      @if (erro()) {
        <div class="erro">{{ erro() }}</div>
      }

      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Título</th>
            <th>WhatsApp</th>
            <th>Status</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (linha of linhas(); track linha.id) {
            <tr [class.local]="linha.origem === 'local'">
              <td>{{ linha.nomeCompleto }}</td>
              <td>{{ linha.tituloEleitor }}</td>
              <td>{{ linha.telefoneWhatsapp || '—' }}</td>
              <td>
                @if (linha.origem === 'servidor') {
                  <span class="badge ok">sincronizado</span>
                } @else if (linha.status === 'PENDING' || linha.status === 'SENT') {
                  <span class="badge pend">pendente</span>
                } @else if (linha.status === 'CONFLICT') {
                  <span class="badge conflict">conflito</span>
                } @else if (linha.status === 'ERROR') {
                  <span class="badge err">erro</span>
                } @else {
                  <span class="badge ok">confirmado</span>
                }
              </td>
              <td>
                @if (linha.origem === 'servidor') {
                  <a [routerLink]="['/eleitores', linha.id]">Abrir</a>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5" class="vazio">Nenhum eleitor cadastrado ainda.</td>
            </tr>
          }
        </tbody>
      </table>

      @if (mostrandoNovo()) {
        <div class="modal-back" (click)="fecharNovo()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Novo eleitor</h3>
            <form (submit)="submeter($event)">
              <label>
                Nome completo *
                <input type="text" [(ngModel)]="form.nomeCompleto" name="nomeCompleto" required />
              </label>
              <label>
                Título de eleitor *
                <input type="text" [(ngModel)]="form.tituloEleitor" name="tituloEleitor" required />
              </label>
              <label>
                WhatsApp
                <input type="tel" [(ngModel)]="form.telefoneWhatsapp" name="telefoneWhatsapp" placeholder="+5511999999999" />
              </label>
              <label>
                Endereço
                <input type="text" [(ngModel)]="form.endereco" name="endereco" />
              </label>
              <label class="row">
                <input type="checkbox" [(ngModel)]="capturarGeoloc" name="capturarGeoloc" />
                <span>Capturar geolocalização automaticamente</span>
              </label>
              @if (geoCapturada()) {
                <small>Geo: {{ geoCapturada()?.latitude?.toFixed(5) }}, {{ geoCapturada()?.longitude?.toFixed(5) }}</small>
              }
              <div class="acoes">
                <button type="button" (click)="fecharNovo()">Cancelar</button>
                <button type="submit" class="primary" [disabled]="submetendo()">
                  {{ submetendo() ? 'Salvando...' : 'Salvar (offline-first)' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
      .acoes-topo { display: flex; gap: 8px; }
      table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; }
      th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e2e8f0; }
      th { background: #f1f5f9; font-size: 13px; color: #475569; }
      tr.local td { background: #fefce8; }
      .vazio { color: #94a3b8; text-align: center; padding: 24px; }
      .badge { padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
      .badge.ok { background: #dcfce7; color: #166534; }
      .badge.pend { background: #fef3c7; color: #92400e; }
      .badge.conflict { background: #fecaca; color: #991b1b; }
      .badge.err { background: #fee2e2; color: #991b1b; }
      button { padding: 8px 16px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; }
      button.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      button:disabled { opacity: 0.6; cursor: not-allowed; }
      .modal-back { position: fixed; inset: 0; background: rgba(15,23,42,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; }
      .modal { background: #fff; padding: 24px; border-radius: 8px; width: 460px; max-width: calc(100vw - 32px); }
      .modal h3 { margin-top: 0; }
      form label { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; font-size: 14px; color: #334155; }
      form label input[type="text"], form label input[type="tel"] { padding: 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
      form label.row { flex-direction: row; align-items: center; gap: 8px; }
      .acoes { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
      .erro { background: #fee2e2; color: #991b1b; padding: 10px; border-radius: 4px; margin-bottom: 12px; }
      small { color: #64748b; }
    `,
  ],
})
export class EleitoresComponent implements OnInit, OnDestroy {
  private eleitorSvc = inject(EleitorService);
  private store = inject(OfflineStore);
  private sync = inject(SyncService);
  private geo = inject(GeolocationService);
  private auth = inject(AuthService);

  linhasServidor = signal<Eleitor[]>([]);
  linhasLocais = signal<EleitorLocal[]>([]);
  outbox = signal<OutboxItem[]>([]);
  erro = signal<string | null>(null);
  mostrandoNovo = signal(false);
  submetendo = signal(false);
  capturarGeoloc = true;
  geoCapturada = signal<{ latitude: number; longitude: number } | null>(null);
  form = {
    nomeCompleto: '',
    tituloEleitor: '',
    telefoneWhatsapp: '',
    endereco: '',
  };
  private timerSync?: ReturnType<typeof setInterval>;

  linhas = () => this.combinarLinhas();
  temPendencias = () => this.outbox().some(o => o.status === 'PENDING' || o.status === 'SENT');
  pendentesCount = () => this.outbox().filter(o => o.status === 'PENDING' || o.status === 'SENT').length;

  async ngOnInit(): Promise<void> {
    await this.recarregar();
    this.timerSync = setInterval(() => this.tentarDrenagem(), 15_000);
    window.addEventListener('online', this.aoVoltarOnline);
  }

  ngOnDestroy(): void {
    if (this.timerSync) clearInterval(this.timerSync);
    window.removeEventListener('online', this.aoVoltarOnline);
  }

  private aoVoltarOnline = () => this.tentarDrenagem();

  async abrirNovo(): Promise<void> {
    this.mostrandoNovo.set(true);
    this.form = { nomeCompleto: '', tituloEleitor: '', telefoneWhatsapp: '', endereco: '' };
    this.geoCapturada.set(null);
    if (this.capturarGeoloc) {
      const g = await this.geo.obterPosicao();
      this.geoCapturada.set(g);
    }
  }

  fecharNovo(): void {
    this.mostrandoNovo.set(false);
  }

  async submeter(e: Event): Promise<void> {
    e.preventDefault();
    if (!this.form.nomeCompleto || !this.form.tituloEleitor) return;
    this.submetendo.set(true);
    this.erro.set(null);
    const clientId = uuid();
    const payload: CadastrarEleitorPayload = {
      nomeCompleto: this.form.nomeCompleto.trim(),
      tituloEleitor: this.form.tituloEleitor.trim(),
      telefoneWhatsapp: this.form.telefoneWhatsapp?.trim() || null,
      endereco: this.form.endereco?.trim() || null,
      geolocalizacao: this.geoCapturada() ?? null,
    };
    try {
      await this.store.eleitores_locais.add({
        clientId,
        tituloEleitor: payload.tituloEleitor,
        payload,
        sincronizado: false,
        criadoEm: new Date().toISOString(),
      });
      await this.sync.enfileirar({
        clientOpId: clientId,
        entidade: 'eleitor',
        operacao: 'CREATE',
        payload,
        timestampLocal: new Date().toISOString(),
      });
      this.mostrandoNovo.set(false);
      await this.recarregar();
      // Dispara sync sem bloquear UI
      this.tentarDrenagem();
    } catch (err) {
      this.erro.set('Falha ao salvar localmente: ' + (err as Error).message);
    } finally {
      this.submetendo.set(false);
    }
  }

  async sincronizarAgora(): Promise<void> {
    await this.tentarDrenagem();
  }

  private async tentarDrenagem(): Promise<void> {
    try {
      const n = await this.sync.drenar();
      if (n > 0) await this.recarregar();
    } catch {
      /* silencioso */
    }
  }

  private async recarregar(): Promise<void> {
    try {
      if (this.auth.isAuthenticated()) {
        const remotos = await firstValueFrom(this.eleitorSvc.listar());
        this.linhasServidor.set(remotos ?? []);
      }
    } catch {
      /* offline ou sem permissão */
    }
    this.linhasLocais.set(await this.store.eleitores_locais.toArray());
    this.outbox.set(await this.store.outbox.toArray());
  }

  private combinarLinhas(): LinhaEleitor[] {
    const remoto: LinhaEleitor[] = this.linhasServidor().map(e => ({
      origem: 'servidor',
      id: e.id,
      nomeCompleto: e.anonimizado ? `Eleitor anonimizado #${e.id}` : e.nomeCompleto,
      tituloEleitor: e.tituloEleitor,
      telefoneWhatsapp: e.telefoneWhatsapp,
    }));
    const outboxPorCliente = new Map(this.outbox().map(o => [o.clientOpId, o]));
    const locais: LinhaEleitor[] = this.linhasLocais()
      .filter(l => !l.serverId)
      .map(l => {
        const ob = outboxPorCliente.get(l.clientId);
        return {
          origem: 'local' as const,
          id: 'local-' + l.clientId,
          nomeCompleto: l.tituloEleitor === (l.payload as CadastrarEleitorPayload)?.tituloEleitor
            ? (l.payload as CadastrarEleitorPayload).nomeCompleto
            : l.tituloEleitor,
          tituloEleitor: l.tituloEleitor,
          telefoneWhatsapp: (l.payload as CadastrarEleitorPayload)?.telefoneWhatsapp ?? null,
          status: ob?.status,
          clientId: l.clientId,
        };
      });
    return [...locais, ...remoto];
  }

}
