import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import Aura from '@primeuix/themes/aura';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    providePrimeNG({
      inputVariant: 'outlined',
      ripple: true,
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.terra-dark'
        }
      }
    }),
    provideRouter(routes)
  ]
};
