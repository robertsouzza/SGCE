import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { RealtimeService, TempoRealEvento } from './realtime.service';
import { AuthService } from '../auth/auth.service';
import { Usuario } from '../../shared/types/user';

const authFake = () => {
  const _user = signal<Usuario | null>(null);
  return {
    _user,
    user: _user.asReadonly(),
    perfil: () => _user()?.perfil ?? null,
    isAuthenticated: () => _user() !== null,
  };
};

describe('RealtimeService', () => {
  let auth: ReturnType<typeof authFake>;
  let svc: RealtimeService;

  beforeEach(() => {
    auth = authFake();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }],
    });
    svc = TestBed.inject(RealtimeService);
  });

  afterEach(() => svc.disconnect());

  it('activate() é no-op para usuário sem partido (SUPER_ADMIN sem sessão)', () => {
    auth._user.set({ id: 1, nome: 'x', email: 'x@x', perfil: 'SUPER_ADMIN_PLATAFORMA', partidoId: null });
    // não deve lançar nem tentar abrir conexão
    expect(() => svc.activate()).not.toThrow();
  });

  it('activate() ignora quando não há usuário logado', () => {
    expect(() => svc.activate()).not.toThrow();
  });

  it('achata mensagem heartbeat_membro direto do backend', done => {
    auth._user.set({ id: 1, nome: 'x', email: 'x@x', perfil: 'MEMBRO_EQUIPE', partidoId: 42 });
    const sub = svc.eventos().subscribe((ev: TempoRealEvento) => {
      expect(ev.tipo).toBe('heartbeat_membro');
      expect(ev.membroId).toBe(7);
      sub.unsubscribe();
      done();
    });
    // Chama o handler privado por reflexão — validação do parser é o objetivo.
    const raw = {
      tipo: 'heartbeat_membro',
      membroId: 7,
      partidoId: 42,
      geolocalizacao: { latitude: -23.5, longitude: -46.6 },
      statusConexao: 'ONLINE',
      timestamp: '2026-09-22T12:00:00Z',
    };
    (svc as unknown as { onMensagem: (m: { body: string }) => void }).onMensagem({
      body: JSON.stringify(raw),
    });
  });

  it('encapsula eventos com envelope (abordagem_sincronizada)', done => {
    auth._user.set({ id: 1, nome: 'x', email: 'x@x', perfil: 'ADMIN', partidoId: 42 });
    const sub = svc.eventos().subscribe(ev => {
      expect(ev.tipo).toBe('abordagem_sincronizada');
      expect(ev.payload).toEqual({ abordagemId: 99 });
      sub.unsubscribe();
      done();
    });
    const raw = { tipo: 'abordagem_sincronizada', partidoId: 42, payload: { abordagemId: 99 } };
    (svc as unknown as { onMensagem: (m: { body: string }) => void }).onMensagem({
      body: JSON.stringify(raw),
    });
  });
});
