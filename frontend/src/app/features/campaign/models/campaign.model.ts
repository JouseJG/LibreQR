import { Page } from '../../../core/models/api.models';

export type CampaignStatus = 'active' | 'paused' | 'finished';

/** Espejo de `CampaignResponse` del backend (id = UUID string). */
export type Campaign = {
  id: string;
  name: string;
  description: string | null;
  status: CampaignStatus;
  createdAt: string;
  updatedAt: string;
};

export type CampaignPage = Page<Campaign>;

/** Espejo de `CampaignUpsertRequest`. */
export type CampaignUpsert = {
  name: string;
  description?: string | null;
};

/** Espejo de `CampaignQrResponse` (relacion campana-QR). */
export type CampaignQrLink = {
  campaignId: string;
  qrCodeId: string;
  addedAt: string;
};

export const CAMPAIGN_STATUS_LABELS: Record<CampaignStatus, string> = {
  active: 'Activa',
  paused: 'Pausada',
  finished: 'Finalizada',
};

export function campaignStatusTone(
  status: CampaignStatus,
): 'success' | 'warning' | 'purple' {
  switch (status) {
    case 'active':
      return 'success';
    case 'paused':
      return 'warning';
    case 'finished':
      return 'purple';
  }
}
