import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Injeta o header X-XSRF-TOKEN em POST/PUT/PATCH/DELETE, lendo o valor
 * do cookie XSRF-TOKEN (não-httpOnly) que o backend emite via CookieCsrfTokenRepository.
 */
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const method = req.method.toUpperCase();
  if (method === 'GET' || method === 'HEAD' || method === 'OPTIONS') {
    return next(req);
  }
  const token = readCookie('XSRF-TOKEN');
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'X-XSRF-TOKEN': token } }));
};

function readCookie(name: string): string | null {
  const cookies = document.cookie ? document.cookie.split('; ') : [];
  for (const c of cookies) {
    const eq = c.indexOf('=');
    if (eq > 0 && decodeURIComponent(c.substring(0, eq)) === name) {
      return decodeURIComponent(c.substring(eq + 1));
    }
  }
  return null;
}
