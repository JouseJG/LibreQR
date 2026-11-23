import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, UiButtonComponent],
  template: `
    <main class="auth-shell">
      <section class="auth-card">
        <header class="auth-header">
          <h1>Iniciar sesión</h1>
          <p>Accede a tu cuenta para gestionar tus QRs y campañas.</p>
        </header>

        <form class="auth-form" (ngSubmit)="submit()">
          <div class="field">
            <label for="login-email">Email <span>*</span></label>
            <input
              id="login-email"
              name="email"
              type="email"
              autocomplete="email"
              placeholder="tu@correo.com"
              required
              [ngModel]="email()"
              (ngModelChange)="email.set($event)" />
          </div>

          <div class="field">
            <label for="login-password">Contraseña <span>*</span></label>
            <input
              id="login-password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="Mínimo 8 caracteres"
              required
              [ngModel]="password()"
              (ngModelChange)="password.set($event)" />
          </div>

          @if (error()) {
            <p class="form-error" role="alert">{{ error() }}</p>
          }

          <ui-button variant="primary" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Entrando…' : 'Entrar' }}
          </ui-button>
        </form>

        <p class="auth-alt">
          ¿No tienes cuenta? <a routerLink="/register">Regístrate</a>
        </p>
      </section>
    </main>
  `,
  styleUrl: './auth-pages.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly error = signal('');
  protected readonly submitting = signal(false);

  protected async submit(): Promise<void> {
    if (!this.email().trim() || !this.password() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    try {
      await this.auth.login({ email: this.email().trim(), password: this.password() });
      const redirect = this.route.snapshot.queryParamMap.get('redirect');
      await this.router.navigateByUrl(redirect || '/dashboard');
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo iniciar sesión');
    } finally {
      this.submitting.set(false);
    }
  }
}
