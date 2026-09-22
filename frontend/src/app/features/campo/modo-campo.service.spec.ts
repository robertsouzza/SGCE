import '@angular/compiler';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ModoCampoService } from './modo-campo.service';
import { GeolocationService } from '../../shared/geolocation/geolocation.service';

class GeoFake {
  proximoResultado: { latitude: number; longitude: number } | null = { latitude: -23.5, longitude: -46.6 };
  async obterPosicao(): Promise<{ latitude: number; longitude: number } | null> {
    return this.proximoResultado;
  }
}

describe('ModoCampoService', () => {
  let svc: ModoCampoService;
  let ctrl: HttpTestingController;
  let geo: GeoFake;

  beforeEach(() => {
    geo = new GeoFake();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GeolocationService, useValue: geo },
      ],
    });
    svc = TestBed.inject(ModoCampoService);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    svc.desligar();
    ctrl.verify();
  });

  it('ligar envia heartbeat imediato e marca ativo=true no 200', async () => {
    const p = svc.ligar();
    // Await do geo.obterPosicao() precisa acontecer antes do POST — aguardar
    // dois ticks para que o corpo async do enviarHeartbeat chegue no http.post.
    await new Promise(r => setTimeout(r, 0));
    await new Promise(r => setTimeout(r, 0));
    const req = ctrl.expectOne('/api/tempo-real/heartbeat');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.geolocalizacao.latitude).toBe(-23.5);
    req.flush({});
    await p;
    expect(svc.ativo()).toBe(true);
    expect(svc.ultimoErro()).toBeNull();
  });

  it('403 desliga o toggle e sinaliza erro de consentimento', async () => {
    const p = svc.ligar();
    await new Promise(r => setTimeout(r, 0));
    await new Promise(r => setTimeout(r, 0));
    const req = ctrl.expectOne('/api/tempo-real/heartbeat');
    req.flush('sem consentimento', { status: 403, statusText: 'Forbidden' });
    await p;
    expect(svc.ativo()).toBe(false);
    expect(svc.ultimoErro()).toContain('consentimento');
  });

  it('sem geolocalização não envia heartbeat e sinaliza erro', async () => {
    geo.proximoResultado = null;
    await svc.ligar();
    ctrl.expectNone('/api/tempo-real/heartbeat');
    expect(svc.ativo()).toBe(false);
    expect(svc.ultimoErro()).toContain('Geolocalização');
  });
});
