import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, UiButtonComponent],
  template: `
    <main class="auth-shell">
      <section class="auth-card">
        <header class="auth-header">
          <h1>Crear cuenta</h1>
          <p>Crea tu cuenta para empezar a generar QRs con métricas.</p>
        </header>

        <form class="auth-form" (ngSubmit)="submit()">
          <div class="field">
            <label for="register-name">Nombre</label>
            <input
              id="register-name"
              name="name"
              autocomplete="name"
              placeholder="Opcional"
              [ngModel]="name()"
              (ngModelChange)="name.set($event)" />
          </div>

          <div class="field">
            <label for="register-email">Email <span>*</span></label>
            <input
              id="register-email"
              name="email"
              type="email"
              autocomplete="email"
              placeholder="tu@correo.com"
              required
              [ngModel]="email()"
              (ngModelChange)="email.set($event)" />
          </div>

          <div class="field">
            <label for="register-password">Contraseña <span>*</span></label>
            <input
              id="register-password"
              name="password"
              type="password"
              autocomplete="new-password"
              placeholder="Mínimo 8 caracteres"
              minlength="8"
              required
              [ngModel]="password()"
              (ngModelChange)="password.set($event)" />
          </div>

          @if (error()) {
            <p class="form-error" role="alert">{{ error() }}</p>
          }
          @if (done()) {
            <p class="form-success" role="status">
              Cuenta creada. <a routerLink="/login">Inicia sesión</a> para continuar.
            </p>
          }

          <ui-button variant="primary" type="submit" [disabled]="submitting() || done()">
            {{ submitting() ? 'Creando…' : 'Crear cuenta' }}
          </ui-button>
        </form>

        <p class="auth-alt">
          ¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a>
        </p>
      </section>
    </main>
  `,
  styleUrl: '../login/auth-pages.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);

  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly error = signal('');
  protected readonly submitting = signal(false);
  protected readonly done = signal(false);

  protected async submit(): Promise<void> {
    if (!this.email().trim() || (this.password().length ?? 0) < 8 || this.submitting()) {
      this.error.set('Email y contraseña de 8+ caracteres son obligatorios.');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    try {
      await this.auth.register({
        email: this.email().trim(),
        name: this.name().trim() || undefined,
        password: this.password(),
      });
      this.done.set(true);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo crear la cuenta');
    } finally {
      this.submitting.set(false);
    }
  }
}
