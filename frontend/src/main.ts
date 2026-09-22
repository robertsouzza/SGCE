// sockjs-client é escrito para Node.js e referencia `global` no bundle. No
// navegador essa variável não existe e a app quebra ao inicializar. Este
// shim tem que rodar ANTES de qualquer import que carregue sockjs.
(window as unknown as { global: Window }).global = window;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
