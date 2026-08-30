import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Anexa withCredentials=true em toda request para o backend, para o cookie
 * httpOnly de sessão (sgce_access) viajar automaticamente.
 */
export const withCredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
