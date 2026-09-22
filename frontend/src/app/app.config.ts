import { ApplicationConfig, inject, isDevMode, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { csrfInterceptor } from './core/http/csrf.interceptor';
import { withCredentialsInterceptor } from './core/http/with-credentials.interceptor';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([withCredentialsInterceptor, csrfInterceptor])),
    // Service Worker registrado mas DESABILITADO por padrão em dev/staging.
    // Habilite via localStorage.setItem('sgce.pwa','on') e recarregue quando
    // quiser testar o modo PWA (offline-first via SW cache). Skill 10 amarra
    // isso a uma flag de env.
    provideServiceWorker('ngsw-worker.js', {
      enabled:
        !isDevMode() &&
        typeof localStorage !== 'undefined' &&
        localStorage.getItem('sgce.pwa') === 'on',
      registrationStrategy: 'registerWhenStable:30000',
    }),
    // Bootstrap: obter cookie XSRF-TOKEN + tentar restaurar sessão via /me
    provideAppInitializer(async () => {
      const auth = inject(AuthService);
      try {
        await firstValueFrom(auth.primeCsrf());
      } catch {
        /* backend indisponível — segue mesmo assim, login vai falhar depois */
      }
      try {
        await firstValueFrom(auth.me());
      } catch {
        /* sem sessão — auth guard leva pra /login */
      }
    }),
  ],
};
