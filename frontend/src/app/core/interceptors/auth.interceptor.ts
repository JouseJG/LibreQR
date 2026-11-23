import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Un 401 de la API significa sesión caducada o ausente: se limpia el estado y se
 * lleva al usuario al login (la cookie la expira el propio backend).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      const onAuthPage = router.url.startsWith('/login') || router.url.startsWith('/register');
      if (error.status === 401 && !onAuthPage) {
        auth.markLoggedOut();
        void router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }
      return throwError(() => error);
    }),
  );
};
