/**
 * Modelos del contrato HTTP del backend (Spring Boot + RFC 7807).
 */

/** Serializacion de `Page<T>` de Spring Data (page `number` es 0-indexed). */
export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** Error RFC 7807 (`ProblemDetail`) devuelto por el backend. */
export type ApiProblem = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
};

/** Espejo de `QrMetricsResponse`. */
export type QrMetrics = {
  totalScans: number;
  humanScans: number;
  botScans: number;
  uniqueVisitors: number;
  dailyAverage: number;
  busiestDay: string | null;
  busiestHour: number | null;
  from: string;
  to: string;
  firstScanAt: string | null;
  lastScanAt: string | null;
  previousPeriodScans: number;
  byCountry: Record<string, number>;
  byDevice: Record<string, number>;
};

/** Espejo de `CampaignMetricsResponse`. */
export type CampaignMetrics = {
  totalScans: number;
  humanScans: number;
  botScans: number;
  uniqueVisitors: number;
  dailyAverage: number;
  from: string;
  to: string;
  previousPeriodScans: number;
  mostScanned: {
    mostName: string | null;
    mostScans: number;
    leastName: string | null;
    leastScans: number;
  } | null;
  activeWithScansPercent: number;
  byCountry: Record<string, number>;
  byDevice: Record<string, number>;
};

/** Espejo de `StatsSummaryResponse` (dashboard). */
export type StatsSummary = {
  scansCurrentMonth: number;
  scansPreviousMonth: number;
  uniqueCurrentMonth: number;
  uniquePreviousMonth: number;
  activeQrs: number;
  totalQrs: number;
  activeCampaigns: number;
  totalCampaigns: number;
};

/** Punto de `daily-scans`: escaneos humanos de un dia del mes en curso. */
export type DailyScanPoint = {
  date: string; // yyyy-MM-dd
  scans: number;
};

/** Escaneos del mes en curso atribuidos a una campana. */
export type CampaignScan = {
  campaignId: string;
  name: string;
  scans: number;
};

/** Extrae un mensaje legible de un error RFC 7807. */
export function problemMessage(error: unknown, fallback = 'Error inesperado'): string {
  const detail = (error as { error?: ApiProblem | null })?.error?.detail;
  return detail || fallback;
}
