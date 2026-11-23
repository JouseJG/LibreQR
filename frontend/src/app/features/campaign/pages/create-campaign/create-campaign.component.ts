import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { problemMessage } from '../../../../core/models/api.models';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';

@Component({
  selector: 'app-create-campaign',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, UiButtonComponent],
  template: `
    <section class="page-shell">
      <a class="back-link" routerLink="/campaigns">← Volver a campañas</a>

      <header class="page-header">
        <div>
          <p class="eyebrow">Campañas</p>
          <h1>Crear campaña</h1>
        </div>
      </header>

      <form class="form-card" (ngSubmit)="submit()">
        <div class="field">
          <label for="campaign-name">Nombre de la campaña <span>*</span></label>
          <input
            id="campaign-name"
            name="name"
            maxlength="120"
            placeholder="Ej. Cartelería verano 2026"
            required
            [ngModel]="name()"
            (ngModelChange)="name.set($event)" />
        </div>

        <div class="field">
          <label for="campaign-description">Descripción</label>
          <textarea
            id="campaign-description"
            name="description"
            rows="4"
            maxlength="500"
            placeholder="Opcional"
            [ngModel]="description()"
            (ngModelChange)="description.set($event)"></textarea>
        </div>

        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }

        <div class="actions">
          <ui-button variant="ghost" routerLink="/campaigns">Cancelar</ui-button>
          <ui-button variant="primary" type="submit" [disabled]="!isValid() || submitting()">
            {{ submitting() ? 'Creando…' : 'Crear campaña' }}
          </ui-button>
        </div>
      </form>
    </section>
  `,
  styleUrl: './create-campaign.component.scss',
})
export class CreateCampaignComponent {
  private readonly campaignApi = inject(CampaignApiService);
  private readonly router = inject(Router);

  protected readonly name = signal('');
  protected readonly description = signal('');
  protected readonly submitting = signal(false);
  protected readonly error = signal('');

  protected isValid(): boolean {
    return this.name().trim().length > 0;
  }

  protected async submit(): Promise<void> {
    if (!this.isValid() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    try {
      const created = await this.campaignApi
        .create({ name: this.name().trim(), description: this.description().trim() || null })
        .toPromise();
      if (created) {
        await this.router.navigate(['/campaigns', created.id]);
      }
    } catch (err) {
      this.error.set(problemMessage(err, 'No se pudo crear la campaña'));
      this.submitting.set(false);
    }
  }
}
