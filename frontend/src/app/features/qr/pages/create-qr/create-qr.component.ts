import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { QrApiService } from '../../../../core/services/qr-api.service';
import { problemMessage } from '../../../../core/models/api.models';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { Campaign } from '../../../campaign/models/campaign.model';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { QrDesignerComponent } from '../../components/qr-designer/qr-designer.component';
import { DEFAULT_QR_DESIGN, designFromConfig, designToConfig, QrDesign } from '../../models/qr-design';
import {
  QR_PAYLOAD_HINTS,
  QR_TYPE_LABELS,
  QR_TYPE_OPTIONS,
  qrShareContent,
  QrItem,
  QrType,
} from '../../models/qr.model';

/**
 * Formulario de QR en dos modos: creacion (/qr/new) y edicion (/qr/:id/edit).
 * En edicion solo cambian nombre, descripcion, destino (QRs dinamicos), campana y
 * diseno: el tipo y el slug (enlace corto) son inmutables.
 */
@Component({
  selector: 'app-create-qr',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, UiButtonComponent, QrDesignerComponent, QrThumbComponent],
  template: `
    <section class="page-shell">
      <a class="back-link" [routerLink]="editingId ? ['/qr', editingId] : '/qr'">
        {{ editingId ? '← Volver al código QR' : '← Volver a códigos QR' }}
      </a>

      <header class="page-header">
        <div>
          <p class="eyebrow">QRs</p>
          <h1>{{ editingId ? 'Editar código QR' : 'Crear código QR' }}</h1>
        </div>
      </header>

      @if (loadingQr()) {
        <p class="preview-hint" role="status">Cargando código QR…</p>
      } @else {
        <div class="create-layout">
          <form class="form-card" (ngSubmit)="submit()">
            <div class="step-title">
              <span class="step-num" aria-hidden="true">1</span>
              <h2>Completa el contenido</h2>
            </div>

            <div class="field">
              <label for="qr-name">Nombre del QR <span>*</span></label>
              <input
                id="qr-name"
                name="name"
                maxlength="120"
                placeholder="Ej. Landing principal"
                [disabled]="isEditing()"
                required
                [ngModel]="name()"
                (ngModelChange)="name.set($event)" />
              @if (isEditing()) {
                <small>El nombre y el tipo no pueden modificarse.</small>
              }
            </div>

            <div class="field">
              <label for="qr-type">Tipo</label>
              <select
                id="qr-type"
                name="type"
                [disabled]="isEditing()"
                [ngModel]="type()"
                (ngModelChange)="type.set($event)">
                @for (option of qrTypeOptions; track option.value) {
                  <option [value]="option.value">{{ option.label }}</option>
                }
              </select>
              @if (isEditing()) {
                <small>El tipo no puede cambiarse: la imagen del QR ya lo codifica.</small>
              }
            </div>

            <div class="field">
              <label for="qr-payload">Contenido <span>*</span></label>
              <input
                id="qr-payload"
                name="payload"
                maxlength="4000"
                [disabled]="payloadLocked()"
                [placeholder]="payloadHint()"
                required
                [ngModel]="payload()"
                (ngModelChange)="payload.set($event)" />
              <small>
                @if (payloadLocked()) {
                  QR estático: el contenido está impreso en la imagen y no puede editarse.
                } @else if (isEditing()) {
                  QR dinámico: el enlace corto no cambia, solo el destino al que redirige.
                } @else {
                  {{ QR_TYPE_LABELS[type()] }}: el formato lo valida el servidor.
                }
              </small>
            </div>

            <div class="field">
              <label for="qr-description">Descripción</label>
              <textarea
                id="qr-description"
                name="description"
                rows="3"
                maxlength="500"
                placeholder="Opcional"
                [ngModel]="description()"
                (ngModelChange)="description.set($event)"></textarea>
            </div>

            <div class="field">
              <label for="qr-campaign">Campaña <span>*</span></label>
              <select
                id="qr-campaign"
                name="campaignId"
                required
                [ngModel]="campaignId()"
                (ngModelChange)="campaignId.set($event)">
                @for (campaign of campaignOptions(); track campaign.id) {
                  <option [value]="campaign.id">{{ campaign.name }}</option>
                }
              </select>
              @if (campaigns().length === 0 && !loadingCampaigns()) {
                <small class="warn">
                  Necesitas al menos una campaña activa.
                  <a routerLink="/campaigns/new">Crea una campaña</a> antes de generar QRs.
                </small>
              }
            </div>

            <div class="step-title">
              <span class="step-num" aria-hidden="true">2</span>
              <h2>Diseña tu código QR</h2>
            </div>

            <app-qr-designer
              [(design)]="design"
              [content]="previewContent()"></app-qr-designer>

            @if (error()) {
              <p class="form-error" role="alert">{{ error() }}</p>
            }

            <div class="actions">
              <ui-button
                variant="ghost"
                [routerLink]="editingId ? ['/qr', editingId] : '/qr'">
                Cancelar
              </ui-button>
              <ui-button variant="primary" type="submit" [disabled]="!isValid() || submitting()">
                {{ submitting() ? 'Guardando…' : isEditing() ? 'Guardar cambios' : 'Crear QR' }}
              </ui-button>
            </div>
          </form>

          <aside class="preview-panel" aria-label="Vista previa del código QR">
            <div class="step-title">
              <span class="step-num" aria-hidden="true">3</span>
              <h2>Descargar código QR</h2>
            </div>

            <div class="preview-box">
              @if (previewContent()) {
                <qr-thumb
                  #bigThumb
                  [content]="previewContent()"
                  [design]="design()"
                  [size]="320"></qr-thumb>
              } @else {
                <div class="preview-placeholder" role="status">
                  Introduce el contenido para previsualizar el QR
                </div>
              }
            </div>

            @if (!isValid()) {
              <p class="preview-hint">Por favor, completa los campos obligatorios.</p>
            } @else if (!isEditing()) {
              <p class="preview-hint preview-hint--ok">
                Listo: puedes descargarlo o crearlo. Al crearlo obtendrás además un enlace
                corto rastreable.
              </p>
            }

            <ui-button
              variant="secondary"
              [disabled]="!previewContent()"
              (click)="downloadPng()">
              Descargar código QR
            </ui-button>
          </aside>
        </div>
      }
    </section>
  `,
  styleUrl: './create-qr.component.scss',
})
export class CreateQrComponent {
  private readonly qrApi = inject(QrApiService);
  private readonly campaignApi = inject(CampaignApiService);
  private readonly router = inject(Router);

  /** Id de la ruta :id/edit; null en modo creacion. */
  protected readonly editingId = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly qrTypeOptions = QR_TYPE_OPTIONS;
  protected readonly QR_TYPE_LABELS = QR_TYPE_LABELS;

  protected readonly name = signal('');
  protected readonly type = signal<QrType>('url');
  protected readonly payload = signal('');
  protected readonly description = signal('');
  protected readonly campaignId = signal('');
  protected readonly submitting = signal(false);
  protected readonly error = signal('');

  protected readonly design = signal<QrDesign>(DEFAULT_QR_DESIGN);
  protected readonly loadingQr = signal(false);
  protected readonly currentEditable = signal(true);
  private loadedQr: QrItem | null = null;

  protected readonly bigThumb = viewChild<QrThumbComponent>('bigThumb');

  protected readonly loadingCampaigns = signal(true);
  protected readonly campaigns = signal<Campaign[]>([]);
  /** Campaña actual aunque no esté activa (edición): se muestra marcada como tal. */
  protected readonly currentCampaign = signal<Campaign | null>(null);

  protected readonly isEditing = computed(() => !!this.editingId);
  protected readonly payloadLocked = computed(() => this.isEditing() && !this.currentEditable());

  protected readonly campaignOptions = computed(() => {
    const options = [...this.campaigns()];
    const current = this.currentCampaign();
    if (current && !options.some((campaign) => campaign.id === current.id)) {
      options.unshift({ ...current, name: `${current.name} (no activa)` });
    }
    return options;
  });

  protected readonly payloadHint = computed(() => QR_PAYLOAD_HINTS[this.type()]);

  /**
   * Contenido del QR dibujado/descargado: en edicion de QRs url es el enlace corto
   * (inmutable); al crear, el payload tecleado.
   */
  protected readonly previewContent = computed(() => {
    if (this.editingId && this.loadedQr && this.loadedQr.type === 'url' && this.loadedQr.slug) {
      return qrShareContent(this.loadedQr);
    }
    return this.payload().trim();
  });

  constructor() {
    const id = this.editingId;
    if (id) {
      this.loadForEdit(id);
    }
    this.campaignApi.list(0, 100, 'name,asc').subscribe({
      next: (page) => {
        this.campaigns.set(page.content.filter((campaign) => campaign.status === 'active'));
        this.loadingCampaigns.set(false);
      },
      error: () => this.loadingCampaigns.set(false),
    });
  }

  private loadForEdit(id: string): void {
    this.loadingQr.set(true);
    this.qrApi.get(id).subscribe({
      next: (qr) => {
        this.loadedQr = qr;
        this.name.set(qr.name);
        this.type.set(qr.type);
        this.payload.set(qr.payload);
        this.description.set(qr.description ?? '');
        this.campaignId.set(qr.campaignId ?? '');
        this.currentEditable.set(qr.editable);
        this.design.set(designFromConfig(qr.designConfig));
        this.loadingQr.set(false);
        if (qr.campaignId) {
          this.campaignApi.get(qr.campaignId).subscribe({
            next: (campaign) => this.currentCampaign.set(campaign),
            error: () => undefined,
          });
        }
      },
      error: () => {
        this.error.set('No se pudo cargar el código QR.');
        this.loadingQr.set(false);
      },
    });
  }

  protected downloadPng(): void {
    if (!this.previewContent()) {
      return;
    }
    const base = this.name().trim() || 'libreqr';
    this.bigThumb()?.download(this.editingId ? `${base}-edit` : base, 'png');
  }

  protected isValid(): boolean {
    return (
      this.name().trim().length > 0 &&
      (this.payloadLocked() ? true : this.payload().trim().length > 0) &&
      this.campaignId().length > 0
    );
  }

  protected async submit(): Promise<void> {
    if (!this.isValid() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    const payloadValue = this.payloadLocked() ? (this.loadedQr?.payload ?? '') : this.payload().trim();
    try {
      const editId = this.editingId;
      if (editId) {
        const updated = await this.qrApi
          .update(editId, {
            type: this.type(),
            name: this.name().trim(),
            description: this.description().trim() || null,
            payload: payloadValue,
            campaignId: this.campaignId(),
            designConfig: designToConfig(this.design()),
          })
          .toPromise();
        await this.router.navigate(['/qr', updated?.id ?? this.editingId]);
      } else {
        const created = await this.qrApi
          .create({
            type: this.type(),
            name: this.name().trim(),
            description: this.description().trim() || null,
            payload: this.payload().trim(),
            campaignId: this.campaignId(),
            designConfig: designToConfig(this.design()),
          })
          .toPromise();
        await this.router.navigate(['/qr/created'], {
          queryParams: created ? { id: created.id } : {},
        });
      }
    } catch (err) {
      this.error.set(problemMessage(err, 'No se pudo guardar el QR'));
      this.submitting.set(false);
    }
  }
}
