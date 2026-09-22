import '@angular/compiler';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EleitorService } from './eleitor.service';

describe('EleitorService', () => {
  let svc: EleitorService;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    svc = TestBed.inject(EleitorService);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('cadastrar envia POST para /api/eleitores com payload', () => {
    svc.cadastrar({ nomeCompleto: 'João', tituloEleitor: 'T1' }).subscribe();
    const req = ctrl.expectOne('/api/eleitores');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nomeCompleto).toBe('João');
    req.flush({ id: 1, nomeCompleto: 'João', tituloEleitor: 'T1' });
  });

  it('anexarAssinatura converte dataUri em multipart', () => {
    const png1x1Base64 =
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEUAAP/n7f8gAAAACklEQVQI12MAAQAABQABtcNMzAAAAABJRU5ErkJggg==';
    const dataUri = 'data:image/png;base64,' + png1x1Base64;
    svc.anexarAssinatura(42, dataUri).subscribe();
    const req = ctrl.expectOne('/api/consentimentos/42/assinatura');
    expect(req.request.method).toBe('POST');
    const form = req.request.body as FormData;
    const file = form.get('arquivo') as File | Blob;
    expect(file).toBeTruthy();
    expect((file as Blob).type).toBe('image/png');
    req.flush({ id: 42 });
  });

  it('gerarDeepLink passa abordagemId e candidatoId como query params', () => {
    svc.gerarDeepLink(10, 20).subscribe();
    const req = ctrl.expectOne(r => r.url === '/api/consentimentos/deep-link-opt-in');
    expect(req.request.params.get('abordagemId')).toBe('10');
    expect(req.request.params.get('candidatoId')).toBe('20');
    req.flush({ url: 'https://wa.me/x', qrCodePngDataUri: 'data:image/png;base64,x', optInId: 'z' });
  });
});
