import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { AppLayoutComponent } from './layout/app-layout.component';

export const routes: Routes = [
  // Rutas publicas de sesion (fuera del layout).
  ...AUTH_ROUTES,
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'campaigns',
        loadChildren: () =>
          import('./features/campaign/campaign.routes').then((m) => m.CAMPAIGN_ROUTES),
      },
      {
        path: 'qr',
        loadChildren: () => import('./features/qr/qr.routes').then((m) => m.QR_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
