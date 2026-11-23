import { QrMetrics, Page } from '../../../core/models/api.models';

export type QrStatus = 'active' | 'inactive';

export type QrType = 'url' | 'text' | 'email' | 'wifi';

/** Espejo de `QrResponse` del backend (id = UUID string). */
export type QrItem = {
  id: string;
  type: QrType;
  status: QrStatus;
  name: string;
  description: string | null;
  slug: string | null;
  payload: string;
  campaignId: string | null;
  designConfig: Record<string, unknown> | null;
  imageUrl: string | null;
  editable: boolean;
  createdAt: string;
  updatedAt: string;
};

export type QrPage = Page<QrItem>;

export type QrMetricsResponse = QrMetrics;

/** Entrada de `QrUpsertRequest` (campaignId obligatoria: todo QR pertenece a una campana). */
export type QrUpsert = {
  type: QrType;
  name: string;
  description?: string | null;
  payload: string;
  campaignId: string;
  designConfig?: Record<string, unknown> | null;
};

export const QR_TYPE_LABELS: Record<QrType, string> = {
  url: 'Enlace web',
  text: 'Texto',
  email: 'Email',
  wifi: 'Wi-Fi',
};

export const QR_STATUS_LABELS: Record<QrStatus, string> = {
  active: 'Activo',
  inactive: 'Inactivo',
};

export const QR_TYPE_OPTIONS: { value: QrType; label: string }[] = (
  Object.keys(QR_TYPE_LABELS) as QrType[]
).map((value) => ({ value, label: QR_TYPE_LABELS[value] }));

export function qrStatusTone(status: QrStatus): 'success' | 'warning' {
  return status === 'active' ? 'success' : 'warning';
}

/** Contenido a codificar en la imagen: enlace corto para QRs url, payload para el resto. */
export function qrShareContent(qr: QrItem): string {
  if (qr.type === 'url' && qr.slug) {
    return `${location.origin}/r/${qr.slug}`;
  }
  return qr.payload;
}

/** Contenido esperado segun el tipo (mismo criterio de validacion del backend). */
export const QR_PAYLOAD_HINTS: Record<QrType, string> = {
  url: 'https://ejemplo.com/pagina',
  text: 'Texto que mostrara el QR al escanearlo',
  email: 'mailto:hola@ejemplo.com?subject=Consulta',
  wifi: 'WIFI:T:WPA;S:mi-red;P:micontraseña;;',
};
