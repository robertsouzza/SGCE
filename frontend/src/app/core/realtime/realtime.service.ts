import { Injectable, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export type TempoRealTipo = 'heartbeat_membro' | 'abordagem_sincronizada' | 'membro_offline_coletando';

export interface TempoRealEvento {
  tipo: TempoRealTipo;
  membroId?: number;
  partidoId?: number;
  geolocalizacao?: { longitude: number; latitude: number };
  statusConexao?: 'ONLINE' | 'OFFLINE_COLETANDO';
  timestamp?: string;
  payload?: unknown;
}

/**
 * Conecta em /ws via SockJS (necessário para o proxy dev-server encaminhar
 * corretamente) e subscribe /topic/tempo-real/{partidoId} — a assinatura só
 * é feita quando há um partidoId conhecido no AuthService (usuário logado
 * como membro/lider/candidato/admin de partido). Auto-reconecta com backoff.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private auth = inject(AuthService);

  private client?: Client;
  private inscricao?: StompSubscription;
  private eventos$ = new Subject<TempoRealEvento>();
  private ativo = false;

  eventos(): Observable<TempoRealEvento> {
    return this.eventos$.asObservable();
  }

  activate(): void {
    if (this.ativo) return;
    const u = this.auth.user();
    if (!u || u.partidoId == null || u.partidoId === '') {
      // SUPER_ADMIN sem sessão de suporte não escuta tópico algum.
      return;
    }
    const partidoId = u.partidoId as number;
    this.ativo = true;

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws') as unknown as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => {
        /* silencioso — trocar para console.log em depuração */
      },
      onConnect: () => {
        this.inscricao = this.client!.subscribe(
          `/topic/tempo-real/${partidoId}`,
          (msg: IMessage) => this.onMensagem(msg),
        );
      },
      onStompError: frame => {
        console.warn('STOMP error', frame.headers['message']);
      },
      onWebSocketClose: () => {
        // reconnectDelay cuida da reconexão automática
      },
    });
    this.client.activate();
  }

  disconnect(): void {
    this.inscricao?.unsubscribe();
    this.inscricao = undefined;
    this.client?.deactivate();
    this.client = undefined;
    this.ativo = false;
  }

  private onMensagem(msg: IMessage): void {
    try {
      const raw = JSON.parse(msg.body);
      // Backend envia:
      //   heartbeat_membro: { tipo, membroId, partidoId, geolocalizacao, statusConexao, timestamp }
      //   demais eventos:   { tipo, partidoId, payload }
      // Achatar ambos em um único shape do frontend.
      if (raw?.tipo === 'heartbeat_membro') {
        this.eventos$.next(raw as TempoRealEvento);
      } else if (raw?.tipo) {
        this.eventos$.next({
          tipo: raw.tipo,
          partidoId: raw.partidoId,
          payload: raw.payload,
        });
      }
    } catch (e) {
      console.warn('Falha ao parsear mensagem STOMP', e);
    }
  }
}
