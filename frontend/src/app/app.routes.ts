import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';
import { ShellComponent } from './core/layout/shell.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/auth/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'partidos',
        canActivate: [roleGuard('SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/partidos.component').then(m => m.PartidosComponent),
      },
      {
        path: 'candidatos',
        canActivate: [roleGuard('ADMIN', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/candidatos.component').then(m => m.CandidatosComponent),
      },
      {
        path: 'equipes',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/equipe/equipes.component').then(m => m.EquipesComponent),
      },
      {
        path: 'financeiro',
        canActivate: [roleGuard('ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO', 'CANDIDATO')],
        loadComponent: () => import('./features/financeiro/financeiro.component').then(m => m.FinanceiroComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
