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
        path: 'partidos/novo',
        canActivate: [roleGuard('SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/partido-novo.component').then(m => m.PartidoNovoComponent),
      },
      {
        path: 'partidos/:id/editar',
        canActivate: [roleGuard('SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/partido-editar.component').then(m => m.PartidoEditarComponent),
      },
      {
        path: 'candidatos',
        canActivate: [roleGuard('ADMIN', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/candidatos.component').then(m => m.CandidatosComponent),
      },
      {
        path: 'candidatos/novo',
        canActivate: [roleGuard('ADMIN', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/candidato-novo.component').then(m => m.CandidatoNovoComponent),
      },
      {
        path: 'candidatos/:id/editar',
        canActivate: [roleGuard('ADMIN', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/partido/candidato-editar.component').then(m => m.CandidatoEditarComponent),
      },
      {
        path: 'equipes',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/equipe/equipes.component').then(m => m.EquipesComponent),
      },
      {
        path: 'equipes/novo',
        canActivate: [roleGuard('ADMIN', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/equipe/equipe-novo.component').then(m => m.EquipeNovoComponent),
      },
      {
        path: 'equipes/:id/editar',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/equipe/equipe-editar.component').then(m => m.EquipeEditarComponent),
      },
      {
        path: 'financeiro',
        canActivate: [roleGuard('ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO', 'CANDIDATO')],
        loadComponent: () => import('./features/financeiro/financeiro.component').then(m => m.FinanceiroComponent),
      },
      {
        path: 'financeiro/recursos/novo',
        canActivate: [roleGuard('ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO')],
        loadComponent: () => import('./features/financeiro/recurso-novo.component').then(m => m.RecursoNovoComponent),
      },
      {
        path: 'financeiro/recursos/:id/editar',
        canActivate: [roleGuard('ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO')],
        loadComponent: () => import('./features/financeiro/recurso-editar.component').then(m => m.RecursoEditarComponent),
      },
      {
        path: 'financeiro/despesas/nova',
        canActivate: [roleGuard('ADMIN', 'GERENTE_FINANCEIRO', 'SECRETARIO')],
        loadComponent: () => import('./features/financeiro/despesa-nova.component').then(m => m.DespesaNovaComponent),
      },
      {
        path: 'eleitores',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE', 'CANDIDATO')],
        loadComponent: () => import('./features/eleitor/eleitores.component').then(m => m.EleitoresComponent),
      },
      {
        path: 'eleitores/:id',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE', 'CANDIDATO')],
        loadComponent: () => import('./features/eleitor/eleitor-detalhe.component').then(m => m.EleitorDetalheComponent),
      },
      {
        path: 'mapa',
        canActivate: [roleGuard('ADMIN', 'LIDER_EQUIPE', 'MEMBRO_EQUIPE', 'CANDIDATO', 'SUPER_ADMIN_PLATAFORMA')],
        loadComponent: () => import('./features/mapa/mapa.component').then(m => m.MapaComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
