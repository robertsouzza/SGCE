import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Perfil } from '../../shared/types/user';
import { ModoCampoService } from '../../features/campo/modo-campo.service';

interface MenuItem {
  label: string;
  rota: string;
  perfis: Perfil[];
}

const MENU: MenuItem[] = [
  { label: 'Dashboard', rota: '/dashboard', perfis: ['SUPER_ADMIN_PLATAFORMA', 'ADMIN', 'CANDIDATO', 'GERENTE_FINANCEIRO', 'SECRETARIO', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE'] },
  { label: 'Partidos', rota: '/partidos', perfis: ['SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Candidatos', rota: '/candidatos', perfis: ['ADMIN', 'SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Equipes', rota: '/equipes', perfis: ['ADMIN', 'LIDER_EQUIPE', 'SUPER_ADMIN_PLATAFORMA'] },
  { label: 'Financeiro', rota: '/financeiro', perfis: ['ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO', 'CANDIDATO'] },
  { label: 'Eleitores', rota: '/eleitores', perfis: ['ADMIN', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE', 'CANDIDATO'] },
  { label: 'Mapa', rota: '/mapa', perfis: ['ADMIN', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE', 'CANDIDATO', 'SUPER_ADMIN_PLATAFORMA'] },
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
            @if (podeModoCampo()) {
              <label class="modo-campo">
                <input type="checkbox" [checked]="campo.ativo()" (change)="toggleCampo($event)" />
                <span>Modo campo</span>
              </label>
              @if (campo.ultimoErro(); as erro) {
                <small class="erro-campo">{{ erro }}</small>
              } @else if (campo.ativo()) {
                <small class="ok-campo">Enviando heartbeat a cada 30s</small>
              }
            }
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
      .user-info { border-top: 1px solid #334155; padding-top: 12px; font-size: 14px; display: flex; flex-direction: column; gap: 6px; }
      .user-info .perfil { color: #94a3b8; font-size: 12px; }
      .user-info button { background: #dc2626; color: #fff; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; }
      .modo-campo { display: flex; align-items: center; gap: 8px; padding: 6px; background: #334155; border-radius: 4px; }
      .modo-campo input { accent-color: #22c55e; }
      .erro-campo { color: #fca5a5; font-size: 11px; }
      .ok-campo { color: #86efac; font-size: 11px; }
      .content { flex: 1; padding: 24px; overflow-y: auto; background: #f8fafc; }
    `,
  ],
})
export class ShellComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  protected campo = inject(ModoCampoService);

  user = this.auth.user;
  menuVisivel = computed<MenuItem[]>(() => {
    const perfil = this.auth.perfil();
    if (!perfil) return [];
    return MENU.filter(m => m.perfis.includes(perfil));
  });
  podeModoCampo = computed(() => {
    const p = this.auth.perfil();
    return p === 'MEMBRO_EQUIPE' || p === 'LIDER_EQUIPE';
  });

  async toggleCampo(e: Event): Promise<void> {
    const alvo = (e.target as HTMLInputElement).checked;
    if (alvo) await this.campo.ligar();
    else this.campo.desligar();
  }

  logout(): void {
    this.campo.desligar();
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
