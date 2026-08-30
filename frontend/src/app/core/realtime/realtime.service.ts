import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

/**
 * Stub do serviço de tempo real. Skill 08 (frontend campo + dashboard)
 * ativa a conexão STOMP via /ws e emite eventos reais nesse Subject.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private eventos$ = new Subject<TempoRealEvento>();

  eventos(): Observable<TempoRealEvento> {
    return this.eventos$.asObservable();
  }

  activate(): void {
    // Skill 08 vai substituir por conexão STOMP em /ws
    // subscribe /topic/tempo-real/{partidoId}
  }

  disconnect(): void {}
}

export interface TempoRealEvento {
  tipo: 'heartbeat_membro' | 'abordagem_sincronizada' | 'membro_offline_coletando';
  [key: string]: unknown;
}
