import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'sgce-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-page">
      <form class="card" (ngSubmit)="entrar()">
        <h1>SGCE — Login</h1>
        <label>
          E-mail
          <input type="email" name="email" [(ngModel)]="email" required autofocus />
        </label>
        <label>
          Senha
          <input type="password" name="senha" [(ngModel)]="senha" required />
        </label>
        <button type="submit" [disabled]="carregando()">
          {{ carregando() ? 'Entrando…' : 'Entrar' }}
        </button>
        @if (erro()) {
          <div class="error">{{ erro() }}</div>
        }
      </form>
    </div>
  `,
  styles: [
    `
      .login-page { min-height: 100vh; display: grid; place-items: center; background: #f1f5f9; }
      .card { width: 380px; }
      h1 { margin: 0 0 16px 0; font-size: 20px; }
    `,
  ],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  senha = '';
  carregando = signal(false);
  erro = signal<string | null>(null);

  entrar(): void {
    this.erro.set(null);
    this.carregando.set(true);
    this.auth.login(this.email, this.senha).subscribe({
      next: () => {
        this.carregando.set(false);
        this.router.navigate(['/']);
      },
      error: err => {
        this.carregando.set(false);
        this.erro.set(err?.error?.mensagem ?? 'Falha no login.');
      },
    });
  }
}
