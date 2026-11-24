import { signal, type WritableSignal } from '@angular/core';
import { BarItem } from '../../shared/components/bar-chart/bar-chart.component';
import { DateRange } from '../../shared/components/date-range/date-range.component';
import { formatRangeLabel } from '../../shared/components/date-range/date-range.component';
import { DonutSlice } from '../../shared/components/donut-chart/donut-chart.component';
import { LinePoint } from '../../shared/components/line-chart/line-chart.component';
import { StatsPreset } from '../../shared/components/stats-overview/stats-overview.component';
import { CategoryCount, DailyScanPoint } from '../models/api.models';

const DEVICE_LABELS: Record<string, string> = {
  mobile: 'Movil',
  desktop: 'Escritorio',
  tablet: 'Tableta',
  unknown: 'Desconocido',
};

const COUNTRY_NAMES = new Intl.DisplayNames('es-ES', { type: 'region' });

/**
 * Estado del rango de medicion de los perfiles (campana/QR): por defecto ultimos
 * 30 dias, presets rapidos y sincronizacion calendario<->preset. Reclama al
 * callback `reload` cuando el usuario cambia el rango.
 */
export class StatsRangeState {
  readonly range: WritableSignal<DateRange>;
  readonly preset = signal('30');
  readonly presets: StatsPreset[] = [
    { value: '7', label: 'Ultimos 7 dias' },
    { value: '30', label: 'Ultimos 30 dias' },
    { value: '90', label: 'Ultimos 90 dias' },
    { value: 'month', label: 'Mes actual' },
    { value: 'custom', label: 'Personalizado' },
  ];

  private readonly today = new Date();

  constructor(private readonly reload: () => void) {
    this.range = signal({
      from: toIsoDate(new Date(this.today.getTime() - 29 * 86_400_000)),
      to: toIsoDate(this.today),
    });
  }

  onRangeChange(range: DateRange): void {
    this.range.set(range);
    this.preset.set('custom');
    this.reload();
  }

  applyPreset(value: string): void {
    let from: Date;
    if (value === 'month') {
      from = new Date(this.today.getFullYear(), this.today.getMonth(), 1);
    } else {
      const days = Number.parseInt(value, 10);
      from = new Date(this.today.getTime() - (days - 1) * 86_400_000);
    }
    this.range.set({ from: toIsoDate(from), to: toIsoDate(this.today) });
    this.preset.set(value);
    this.reload();
  }

  /** Dias inclusivos del rango actual (para la media diaria). */
  get days(): number {
    const range = this.range();
    const ms = Date.parse(`${range.to}T00:00:00Z`) - Date.parse(`${range.from}T00:00:00Z`);
    return Math.max(1, Math.round(ms / 86_400_000) + 1);
  }

  /** Etiqueta legible: nombre del mes si el rango cae en uno solo, intervalo si no. */
  get label(): string {
    const { from, to } = this.range();
    const fromDate = new Date(`${from}T00:00:00`);
    const toDate = new Date(`${to}T00:00:00`);
    if (
      fromDate.getFullYear() === toDate.getFullYear() &&
      fromDate.getMonth() === toDate.getMonth()
    ) {
      return new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' }).format(fromDate);
    }
    return formatRangeLabel(from, to);
  }
}

export function statsNumber(value: number): string {
  return new Intl.NumberFormat('es-ES').format(value);
}

export function statsDailyAverage(scans: number, days: number): number {
  return Math.round((scans / Math.max(1, days)) * 10) / 10;
}

export function statsPercentOfAll(scans: number, scansAllTime: number): string {
  if (scansAllTime === 0) {
    return 'Sin escaneos historicos';
  }
  return `${new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 }).format(
    (scans / scansAllTime) * 100,
  )}% de todos los escaneos`;
}

export function statsTrendPoints(series: DailyScanPoint[]): LinePoint[] {
  return series.map((point) => ({
    label: new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' })
      .format(new Date(`${point.date}T00:00:00Z`))
      .replace('.', ''),
    value: point.scans,
  }));
}

export function statsDeviceSlices(rows: CategoryCount[]): DonutSlice[] {
  return rows.map((row) => ({
    label: DEVICE_LABELS[row.key] ?? row.key,
    value: row.scans,
  }));
}

/** Top 3 paises + "Otros" (resto + desconocidos); sufijo con % sobre el total. */
export function statsLocationItems(rows: CategoryCount[], totalScans: number): BarItem[] {
  if (totalScans === 0) {
    return [];
  }
  const named = rows
    .filter((row) => row.key !== 'unknown')
    .sort((a, b) => b.scans - a.scans);
  const unknown = rows.find((row) => row.key === 'unknown')?.scans ?? 0;
  const others = named.slice(3).reduce((sum, row) => sum + row.scans, 0) + unknown;
  const percent = (value: number) =>
    ` ${new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 }).format(
      (value / totalScans) * 100,
    )}%`;
  const items = named.slice(0, 3).map((row) => ({
    label: countryName(row.key),
    value: row.scans,
    key: row.key,
    suffix: percent(row.scans),
  }));
  if (others > 0) {
    items.push({ label: 'Otros', value: others, key: 'others', suffix: percent(others) });
  }
  return items;
}

function countryName(code: string): string {
  try {
    return COUNTRY_NAMES.of(code.toUpperCase()) ?? code;
  } catch {
    return code;
  }
}

function toIsoDate(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}
