import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss',
})
export class AppLayoutComponent implements OnDestroy {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly breadcrumbs = signal<string[]>(['Dashboard']);

  private readonly unsubscribe = this.router.events
    .pipe(filter((event) => event instanceof NavigationEnd))
    .subscribe(() => this.updateBreadcrumbs());

  constructor() {
    this.updateBreadcrumbs();
  }

  ngOnDestroy(): void {
    this.unsubscribe.unsubscribe();
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  private updateBreadcrumbs(): void {
    const segments = this.router.url.split('?')[0].split('/').filter(Boolean);
    const items: string[] = ['Dashboard'];
    let previousSegment = '';

    for (const segment of segments) {
      if (/^\d+$/.test(segment)) {
        continue;
      }

      switch (segment) {
        case 'dashboard':
          // la raiz ya es 'Dashboard': no duplicar (romperia el track del @for)
          if (items[items.length - 1] !== 'Dashboard') {
            items.push('Dashboard');
          }
          previousSegment = 'dashboard';
          break;
        case 'campaigns':
          items.push('Campañas');
          previousSegment = 'campaigns';
          break;
        case 'qr':
          items.push('Códigos QR');
          previousSegment = 'qr';
          break;
        case 'new':
          items.push(previousSegment === 'campaigns' ? 'Crear campaña' : 'Crear QR');
          previousSegment = 'new';
          break;
        case 'created':
          items.push('Creado');
          previousSegment = 'created';
          break;
        case 'edit':
          items.push('Editar QR');
          previousSegment = 'edit';
          break;
        default:
          break;
      }
    }

    this.breadcrumbs.set(items);
  }
}
