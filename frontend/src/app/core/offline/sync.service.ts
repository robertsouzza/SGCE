import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { OfflineStore, OutboxItem } from './offline.store';

/**
 * Envia lotes acumulados no outbox para POST /api/sincronizacao/lote (skill 04).
 * Detecção real de conectividade: ping ao backend (não confiar só em
 * navigator.onLine, que mente em alguns cenários).
 *
 * Skill 08 adiciona: Background Sync API, notificação de CONFLICT_RESOLVED,
 * retry com backoff exponencial.
 */
@Injectable({ providedIn: 'root' })
export class SyncService {
  private http = inject(HttpClient);
  private store = inject(OfflineStore);

  private static readonly BATCH_MAX = 50;

  async enfileirar(item: Omit<OutboxItem, 'id' | 'status'>): Promise<number> {
    return this.store.outbox.add({ ...item, status: 'PENDING' });
  }

  async temPendencias(): Promise<boolean> {
    return (await this.store.outbox.where('status').equals('PENDING').count()) > 0;
  }

  async pingBackend(): Promise<boolean> {
    try {
      await this.http.head('/actuator/health', { observe: 'response' }).toPromise();
      return true;
    } catch {
      return false;
    }
  }

  /** Drena até BATCH_MAX operações pendentes; retorna número processado. */
  async drenar(): Promise<number> {
    if (!(await this.pingBackend())) return 0;

    const pendentes = await this.store.outbox
      .where('status')
      .equals('PENDING')
      .limit(SyncService.BATCH_MAX)
      .toArray();
    if (pendentes.length === 0) return 0;

    const payload = {
      operacoes: pendentes.map(p => ({
        clientOpId: p.clientOpId,
        entidade: p.entidade,
        operacao: p.operacao,
        payload: p.payload,
        timestampLocal: p.timestampLocal,
      })),
    };
    try {
      const resposta = await this.http
        .post<{ resultados: Array<{ clientOpId: string; status: string; serverId?: number; mensagem?: string }> }>(
          '/api/sincronizacao/lote',
          payload,
        )
        .toPromise();
      const resultados = resposta?.resultados ?? [];
      for (const r of resultados) {
        const item = pendentes.find(p => p.clientOpId === r.clientOpId);
        if (!item?.id) continue;
        const novoStatus: OutboxItem['status'] =
          r.status === 'CREATED' || r.status === 'CONFLICT_RESOLVED' || r.status === 'IDEMPOTENT_OK'
            ? 'CONFIRMED'
            : r.status === 'CLOCK_SKEW'
              ? 'ERROR'
              : 'ERROR';
        await this.store.outbox.update(item.id, {
          status: novoStatus,
          ultimaTentativa: new Date().toISOString(),
          erro: r.mensagem,
        });
      }
      return resultados.length;
    } catch {
      const agora = new Date().toISOString();
      for (const p of pendentes) {
        if (p.id) await this.store.outbox.update(p.id, { ultimaTentativa: agora });
      }
      return 0;
    }
  }
}
