import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { Usuario } from '../../shared/types/user';

/**
 * Autenticação por cookie httpOnly (D-08). Frontend NUNCA vê o token —
 * o cookie viaja transparente porque estamos same-origin via nginx (/api → backend).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private readonly _user = signal<Usuario | null>(null);
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly perfil = computed(() => this._user()?.perfil ?? null);

  /** Solicita cookie XSRF-TOKEN — chame no bootstrap. */
  primeCsrf(): Observable<void> {
    return this.http.get<void>('/api/auth/csrf-token');
  }

  login(email: string, senha: string): Observable<Usuario> {
    return this.http
      .post<Usuario>('/api/auth/login', { email, senha })
      .pipe(tap(u => this._user.set(u)));
  }

  me(): Observable<Usuario | null> {
    return this.http.get<Usuario>('/api/auth/me').pipe(
      tap(u => this._user.set(u)),
      catchError(() => {
        this._user.set(null);
        return of(null);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}).pipe(tap(() => this._user.set(null)));
  }
}
