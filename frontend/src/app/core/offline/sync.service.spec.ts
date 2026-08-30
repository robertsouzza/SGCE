import '@angular/compiler';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OfflineStore } from './offline.store';
import { SyncService } from './sync.service';

describe('SyncService', () => {
  let svc: SyncService;
  let store: OfflineStore;
  let ctrl: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    svc = TestBed.inject(SyncService);
    store = TestBed.inject(OfflineStore);
    ctrl = TestBed.inject(HttpTestingController);
    await store.outbox.clear();
  });

  afterEach(() => ctrl.verify());

  it('enfileirar salva com status PENDING e temPendencias true', async () => {
    await svc.enfileirar({
      clientOpId: 'abc-1',
      entidade: 'eleitor',
      operacao: 'CREATE',
      payload: { tituloEleitor: 'T1' },
      timestampLocal: new Date().toISOString(),
    });
    expect(await svc.temPendencias()).toBe(true);
    const items = await store.outbox.toArray();
    expect(items).toHaveLength(1);
    expect(items[0].status).toBe('PENDING');
  });

  it('drenar processa e marca CONFIRMED para CREATED', async () => {
    await svc.enfileirar({
      clientOpId: 'op-1',
      entidade: 'eleitor',
      operacao: 'CREATE',
      payload: { tituloEleitor: 'T1' },
      timestampLocal: new Date().toISOString(),
    });

    const p = svc.drenar();
    await new Promise(r => setTimeout(r, 0));
    ctrl.expectOne('/actuator/health').flush(null); // ping
    await new Promise(r => setTimeout(r, 0));
    ctrl
      .expectOne('/api/sincronizacao/lote')
      .flush({ resultados: [{ clientOpId: 'op-1', status: 'CREATED', serverId: 42 }] });

    const n = await p;
    expect(n).toBe(1);
    const items = await store.outbox.toArray();
    expect(items[0].status).toBe('CONFIRMED');
  });

  it('drenar marca ERROR para CLOCK_SKEW', async () => {
    await svc.enfileirar({
      clientOpId: 'op-skew',
      entidade: 'eleitor',
      operacao: 'CREATE',
      payload: {},
      timestampLocal: new Date().toISOString(),
    });

    const p = svc.drenar();
    await new Promise(r => setTimeout(r, 0));
    ctrl.expectOne('/actuator/health').flush(null);
    await new Promise(r => setTimeout(r, 0));
    ctrl
      .expectOne('/api/sincronizacao/lote')
      .flush({ resultados: [{ clientOpId: 'op-skew', status: 'CLOCK_SKEW', mensagem: 'relogio' }] });

    await p;
    const items = await store.outbox.toArray();
    expect(items[0].status).toBe('ERROR');
    expect(items[0].erro).toBe('relogio');
  });
});
