import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege el area autenticada. En la primera navegacion sondea la sesion (cookie
 * HttpOnly viva o muerta) antes de decidir; redirige a /login conservando el destino.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.ensureSession();

  if (auth.loggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
};
