import '@angular/compiler';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { csrfInterceptor } from './csrf.interceptor';

describe('csrfInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([csrfInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    // limpa cookies de testes anteriores (jsdom persiste entre casos)
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  afterEach(() => ctrl.verify());

  it('não injeta header em GET', () => {
    http.get('/api/foo').subscribe();
    const r = ctrl.expectOne('/api/foo');
    expect(r.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    r.flush({});
  });

  it('injeta X-XSRF-TOKEN em POST quando cookie XSRF-TOKEN existe', () => {
    document.cookie = 'XSRF-TOKEN=abc-123';
    http.post('/api/foo', {}).subscribe();
    const r = ctrl.expectOne('/api/foo');
    expect(r.request.headers.get('X-XSRF-TOKEN')).toBe('abc-123');
    r.flush({});
  });

  it('não injeta se cookie XSRF-TOKEN ausente', () => {
    http.post('/api/foo', {}).subscribe();
    const r = ctrl.expectOne('/api/foo');
    expect(r.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    r.flush({});
  });
});
