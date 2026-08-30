import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Perfil } from '../../shared/types/user';

interface MenuItem {
  label: string;
  rota: string;
  perfis: Perfil[];
}

const MENU: MenuItem[] = [
  { label: 'Partidos', rota: '/partidos', perfis: ['SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Candidatos', rota: '/candidatos', perfis: ['ADMIN', 'SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Equipes', rota: '/equipes', perfis: ['ADMIN', 'LIDER_EQUIPE', 'SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Financeiro', rota: '/financeiro', perfis: ['ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO', 'CANDIDATO'] },
];

@Component({
  selector: 'sgce-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">SGCE</div>
        <nav>
          @for (item of menuVisivel(); track item.rota) {
            <a [routerLink]="item.rota">{{ item.label }}</a>
          }
        </nav>
        @if (user(); as u) {
          <div class="user-info">
            <div>{{ u.nome }}</div>
            <div class="perfil">{{ u.perfil }}</div>
            <button (click)="logout()">Sair</button>
          </div>
        }
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [
    `
      .shell { display: flex; height: 100vh; }
      .sidebar { width: 240px; background: #1e293b; color: #f1f5f9; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
      .brand { font-size: 24px; font-weight: 700; letter-spacing: 1px; }
      nav { display: flex; flex-direction: column; gap: 4px; margin-top: 20px; flex: 1; }
      nav a { color: #cbd5e1; text-decoration: none; padding: 8px 12px; border-radius: 4px; }
      nav a:hover { background: #334155; color: #fff; }
      .user-info { border-top: 1px solid #334155; padding-top: 12px; font-size: 14px; }
      .user-info .perfil { color: #94a3b8; font-size: 12px; margin-bottom: 8px; }
      .user-info button { background: #dc2626; color: #fff; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; width: 100%; }
      .content { flex: 1; padding: 24px; overflow-y: auto; background: #f8fafc; }
    `,
  ],
})
export class ShellComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  user = this.auth.user;
  menuVisivel = computed<MenuItem[]>(() => {
    const perfil = this.auth.perfil();
    if (!perfil) return [];
    return MENU.filter(m => m.perfis.includes(perfil));
  });

  logout(): void {
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
