import '@angular/compiler';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { Usuario } from '../../shared/types/user';

describe('AuthService', () => {
  let svc: AuthService;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    svc = TestBed.inject(AuthService);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('inicia sem usuário autenticado', () => {
    expect(svc.user()).toBeNull();
    expect(svc.isAuthenticated()).toBe(false);
  });

  it('login popula o signal user e isAuthenticated fica true', () => {
    const u: Usuario = { id: 1, nome: 'X', email: 'x@y', perfil: 'ADMIN', partidoId: 5 };
    svc.login('x@y', 'senha').subscribe();
    ctrl.expectOne('/api/auth/login').flush(u);
    expect(svc.user()).toEqual(u);
    expect(svc.isAuthenticated()).toBe(true);
    expect(svc.perfil()).toBe('ADMIN');
  });

  it('me() bem-sucedido popula user; falha zera', () => {
    const u: Usuario = { id: 1, nome: 'X', email: 'x@y', perfil: 'CANDIDATO', partidoId: 5 };
    svc.me().subscribe();
    ctrl.expectOne('/api/auth/me').flush(u);
    expect(svc.user()).toEqual(u);

    svc.me().subscribe(v => expect(v).toBeNull());
    ctrl.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(svc.user()).toBeNull();
  });

  it('logout zera user', () => {
    const u: Usuario = { id: 1, nome: 'X', email: 'x@y', perfil: 'ADMIN', partidoId: 5 };
    svc.login('x@y', 's').subscribe();
    ctrl.expectOne('/api/auth/login').flush(u);
    expect(svc.isAuthenticated()).toBe(true);

    svc.logout().subscribe();
    ctrl.expectOne('/api/auth/logout').flush(null);
    expect(svc.user()).toBeNull();
    expect(svc.isAuthenticated()).toBe(false);
  });
});
