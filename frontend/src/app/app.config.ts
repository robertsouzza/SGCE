import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
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
