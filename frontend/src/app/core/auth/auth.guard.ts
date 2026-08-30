import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Perfil } from '../../shared/types/user';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  router.navigate(['/login']);
  return false;
};

export const roleGuard =
  (...perfisPermitidos: Perfil[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const perfil = auth.perfil();
    if (perfil && perfisPermitidos.includes(perfil)) return true;
    router.navigate(['/']);
    return false;
  };
