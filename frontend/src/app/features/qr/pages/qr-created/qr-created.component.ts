import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { QrApiService } from '../../../../core/services/qr-api.service';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { PanelComponent } from '../../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { designFromConfig } from '../../models/qr-design';
import {
  QrItem,
  QR_STATUS_LABELS,
  qrStatusTone,
} from '../../models/qr.model';

@Component({
  selector: 'app-qr-created',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    PanelComponent,
    QrThumbComponent,
    StatusBadgeComponent,
    UiButtonComponent,
  ],
  template: `
    <section class="page-shell">
      <div class="created-head">
        <span class="created-check" aria-hidden="true">✓</span>
        <h1>Tu código QR ya está listo</h1>
        <p>Descárgalo, compártelo y empieza a descubrir qué pasa después del escaneo.</p>
      </div>

      @if (qr(); as created) {
        <div class="created-grid">
          <app-panel>
            <div class="created-left">
              <status-badge
                [status]="QR_STATUS_LABELS[created.status]"
                [tone]="qrStatusTone(created.status)"></status-badge>

              <div class="created-qr">
                <qr-thumb
                  #thumb
                  [content]="shortLink()"
                  [design]="designFromConfig(created.designConfig)"
                  [size]="220"></qr-thumb>
              </div>

              <div class="created-caption">
                <strong>{{ created.name }}</strong>
              </div>

              <div class="created-download">
                <ui-button variant="primary" (click)="download('png')">↓ Descargar PNG</ui-button>
                <ui-button variant="secondary" (click)="download('svg')">↓ Descargar SVG</ui-button>
              </div>
              <small class="created-hint">PNG para digital · SVG para impresión</small>
            </div>
          </app-panel>

          <div class="created-right">
            <app-panel>
              <h2>Todo en su sitio</h2>

              <div class="created-info">
                <div>
                  <label>Campaña</label>
                  @if (created.campaignId) {
                    <a class="inline-link" [routerLink]="['/campaigns', created.campaignId]">
                      {{ campaignName() || '—' }} <span aria-hidden="true">→</span>
                    </a>
                  } @else {
                    <span>—</span>
                  }
                </div>

                <div>
                  <label>URL del código QR</label>
                  <div class="created-url">
                    <span class="created-url-text">{{ shortLink() }}</span>
                    <button type="button" class="copy-btn" (click)="copyLink()">
                      {{ copied() ? '¡Copiado!' : 'Copiar' }}
                    </button>
                  </div>
                </div>
              </div>

              <p class="created-meta">
                Creado el {{ created.createdAt | date: 'd MMM yyyy' }} · {{ scanInfo() }}
              </p>
            </app-panel>

            <div class="created-next-panel">
              <h2>El siguiente paso es conectar</h2>
              <ul class="created-next">
                <li>
                  <span class="next-icon" aria-hidden="true">□</span>
                  <span>Prueba el QR desde tu móvil.</span>
                </li>
                <li>
                  <span class="next-icon" aria-hidden="true">◉</span>
                  <span>Colócalo en tu cartel o material impreso.</span>
                </li>
                <li>
                  <span class="next-icon" aria-hidden="true">▤</span>
                  <span>
                    Consulta las primeras escaneadas en
                    <a class="inline-link" [routerLink]="['/qr', created.id]">Estadísticas</a>.
                  </span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      } @else if (loading()) {
        <p class="created-hint created-center" role="status">Cargando código QR…</p>
      } @else {
        <p class="created-hint created-center">
          El QR se creó correctamente, pero no se pudo cargar su detalle.
        </p>
      }

      <div class="created-actions">
        <ui-button variant="secondary" routerLink="/qr/new">+ Crear otro código</ui-button>
        <ui-button variant="primary" routerLink="/qr">Ver todos los códigos</ui-button>
      </div>
    </section>
  `,
  styleUrl: './qr-created.component.scss',
})
export class QrCreatedComponent {
  private readonly qrApi = inject(QrApiService);
  private readonly campaignApi = inject(CampaignApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly qr = signal<QrItem | null>(null);
  protected readonly loading = signal(true);
  protected readonly copied = signal(false);
  protected readonly campaignName = signal('');
  protected readonly humanScans = signal<number | null>(null);

  protected readonly designFromConfig = designFromConfig;
  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly qrStatusTone = qrStatusTone;

  private readonly thumb = viewChild<QrThumbComponent>('thumb');

  protected readonly shortLink = computed(() => {
    const created = this.qr();
    return created?.slug ? `${location.origin}/r/${created.slug}` : (created?.payload ?? '');
  });

  protected readonly scanInfo = computed(() => {
    const scans = this.humanScans();
    return scans && scans > 0 ? `${scans} escaneos humanos` : 'Aún sin escaneos';
  });

  constructor() {
    const id = this.route.snapshot.queryParamMap.get('id');
    if (!id) {
      this.loading.set(false);
      return;
    }
    this.qrApi.get(id).subscribe({
      next: (qr) => {
        this.qr.set(qr);
        this.loading.set(false);
        this.loadCampaign(qr.campaignId);
        this.qrApi.scanCounts([qr.id]).subscribe({
          next: (counts) => this.humanScans.set(counts[qr.id] ?? 0),
          error: () => this.humanScans.set(null),
        });
      },
      error: () => this.loading.set(false),
    });
  }

  protected download(extension: 'png' | 'svg'): void {
    const created = this.qr();
    this.thumb()?.download(created?.name || 'libreqr', extension);
  }

  protected async copyLink(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.shortLink());
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.copied.set(false);
    }
  }

  private loadCampaign(campaignId: string | null): void {
    if (!campaignId) {
      return;
    }
    this.campaignApi.get(campaignId).subscribe({
      next: (campaign) => this.campaignName.set(campaign.name),
      error: () => this.campaignName.set(''),
    });
  }
}
