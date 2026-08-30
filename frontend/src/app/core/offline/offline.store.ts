import { Injectable } from '@angular/core';
import Dexie, { Table } from 'dexie';

/** Uma operação enfileirada offline pendente de sync. */
export interface OutboxItem {
  id?: number;
  clientOpId: string;
  entidade: 'eleitor' | 'abordagem' | 'consentimento';
  operacao: 'CREATE' | 'UPDATE';
  payload: unknown;
  timestampLocal: string;
  status: 'PENDING' | 'SENT' | 'CONFIRMED' | 'ERROR' | 'CONFLICT';
  ultimaTentativa?: string;
  erro?: string;
}

/** Cadastro offline de eleitor (skill 08 alimenta essa tabela). */
export interface EleitorLocal {
  id?: number;
  clientId: string;
  serverId?: number;
  tituloEleitor: string;
  payload: unknown;
  sincronizado: boolean;
  criadoEm: string;
}

/**
 * IndexedDB via Dexie. Skill 08 (frontend de campo) alimenta as tabelas
 * eleitores_locais/abordagens_locais/consentimentos_locais e chama SyncService.
 * Aqui na skill 07 já criamos o schema e outbox base para essa integração.
 */
@Injectable({ providedIn: 'root' })
export class OfflineStore extends Dexie {
  outbox!: Table<OutboxItem, number>;
  eleitores_locais!: Table<EleitorLocal, number>;

  constructor() {
    super('sgce-offline');
    this.version(1).stores({
      outbox: '++id, clientOpId, status, entidade',
      eleitores_locais: '++id, clientId, tituloEleitor, sincronizado',
    });
  }
}
