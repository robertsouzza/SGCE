import { CommonModule } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { Subscription } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { RealtimeService, TempoRealEvento } from '../../core/realtime/realtime.service';
import { MapaComponent } from '../mapa/mapa.component';

interface UltimoEvento {
  ts: string;
  descricao: string;
}

/**
 * Painel + mapa lateral (RF-18 a RF-20). Subscribe eventos do RealtimeService
 * e reflete no mapa em tempo real: heartbeats movem markers, offline muda cor,
 * abordagens sincronizadas incrementam contador.
 */
@Component({
  selector: 'sgce-dashboard',
  standalone: true,
  imports: [CommonModule, MapaComponent],
  template: `
    <h2>Painel</h2>
    @if (auth.user(); as u) {
      <p>Bem-vindo, <strong>{{ u.nome }}</strong> ({{ u.perfil }}).</p>
    }

    <div class="grid">
      <div class="cards">
        <div class="card">
          <div class="valor">{{ heartbeatsCount() }}</div>
          <div class="rotulo">Heartbeats recebidos</div>
        </div>
        <div class="card">
          <div class="valor">{{ abordagensCount() }}</div>
          <div class="rotulo">Abordagens sincronizadas</div>
        </div>
        <div class="card">
          <div class="valor">{{ membrosOfflineCount() }}</div>
          <div class="rotulo">Membros offline coletando</div>
        </div>
      </div>

      <sgce-mapa #mapa />

      <div class="log">
        <strong>Últimos eventos</strong>
        <ul>
          @for (ev of ultimos(); track $index) {
            <li><small>{{ ev.ts }}</small> — {{ ev.descricao }}</li>
          } @empty {
            <li class="vazio">Nenhum evento ainda. Ative o modo campo em outro dispositivo/aba.</li>
          }
        </ul>
      </div>
    </div>
  `,
  styles: [
    `
      .grid { display: grid; grid-template-columns: 260px 1fr; grid-template-rows: auto 1fr; gap: 16px; margin-top: 12px; }
      .cards { grid-column: 1; grid-row: 1 / span 2; display: flex; flex-direction: column; gap: 12px; }
      sgce-mapa { grid-column: 2; grid-row: 1; height: 480px; }
      .log { grid-column: 1 / span 2; background: #fff; padding: 12px; border-radius: 6px; max-height: 200px; overflow-y: auto; }
      .card { background: #fff; padding: 16px; border-radius: 6px; text-align: center; }
      .card .valor { font-size: 28px; font-weight: 700; color: #2563eb; }
      .card .rotulo { color: #64748b; font-size: 13px; }
      .log ul { list-style: none; padding: 0; margin: 8px 0 0; font-size: 13px; }
      .log li { padding: 4px 0; border-bottom: 1px solid #f1f5f9; }
      .log li.vazio { color: #94a3b8; font-style: italic; }
    `,
  ],
})
export class DashboardComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  private rt = inject(RealtimeService);
  @ViewChild('mapa') mapa?: MapaComponent;

  heartbeatsCount = signal(0);
  abordagensCount = signal(0);
  membrosOfflineCount = signal(0);
  ultimos = signal<UltimoEvento[]>([]);
  private sub?: Subscription;

  ngOnInit(): void {
    this.rt.activate();
    this.sub = this.rt.eventos().subscribe(ev => this.onEvento(ev));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.rt.disconnect();
  }

  private onEvento(ev: TempoRealEvento): void {
    const ts = new Date().toLocaleTimeString();
    if (ev.tipo === 'heartbeat_membro' && ev.membroId != null && ev.geolocalizacao) {
      this.heartbeatsCount.update(v => v + 1);
      this.mapa?.atualizarMembro(
        ev.membroId,
        ev.geolocalizacao.latitude,
        ev.geolocalizacao.longitude,
        ev.statusConexao === 'OFFLINE_COLETANDO' ? '#f59e0b' : '#2563eb',
      );
      this.pushEvento({ ts, descricao: `Heartbeat membro #${ev.membroId}` });
    } else if (ev.tipo === 'abordagem_sincronizada') {
      this.abordagensCount.update(v => v + 1);
      this.pushEvento({ ts, descricao: 'Abordagem sincronizada' });
    } else if (ev.tipo === 'membro_offline_coletando') {
      this.membrosOfflineCount.update(v => v + 1);
      const membroId = (ev.payload as { membroId?: number })?.membroId;
      if (membroId != null) this.mapa?.marcarMembroOffline(membroId);
      this.pushEvento({ ts, descricao: `Membro #${membroId ?? '?'} offline coletando` });
    }
  }

  private pushEvento(e: UltimoEvento): void {
    this.ultimos.update(list => [e, ...list].slice(0, 20));
  }
}
