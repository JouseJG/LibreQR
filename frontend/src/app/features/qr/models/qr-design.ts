/**
 * Configuracion de diseno del QR, mapeada a qr-code-styling:
 *
 * - MARCO: banda de texto bajo el QR (`labelOptions`) con su propio color de texto.
 * - FORMA: trama de puntos (`dotsOptions`: color o degradado, fondo opcionalmente
 *   transparente), esquinas (`cornersSquareOptions` con color propio) y centro de las
 *   esquinas (`cornersDotOptions` con color propio).
 * - LOGO: imagen central subida por el usuario (`image` en dataURL) con mascara
 *   opcional para eliminar el fondo detras del logo (`logoColor`).
 *
 * Se serializa en `designConfig` (Map opaco para el backend). Referencias estables
 * (cache por JSON) para no re-renderizar en cada deteccion de cambios.
 */

export type QrFrameStyle = 'none' | 'text';
export type QrDotType = 'square' | 'rounded' | 'classy' | 'classy-rounded' | 'dots';
export type QrCornerType = 'square' | 'dot' | 'extra-rounded';
export type QrCornerDotType = 'square' | 'dot';
export type QrLogoSource = 'none' | 'brand' | 'custom';

export type QrDesign = {
  frame: QrFrameStyle;
  frameText: string;
  frameTextColor: string;
  dotType: QrDotType;
  dotColor: string;
  background: string;
  cornerType: QrCornerType;
  cornerColor: string;
  cornerDotType: QrCornerDotType;
  cornerDotColor: string;
  logo: QrLogoSource;
  logoData: string;
};

export const DEFAULT_QR_DESIGN: QrDesign = {
  frame: 'none',
  frameText: '',
  frameTextColor: '#0d9995',
  dotType: 'rounded',
  dotColor: '#17232f',
  background: '#ffffff',
  cornerType: 'extra-rounded',
  cornerColor: '#17232f',
  cornerDotType: 'dot',
  cornerDotColor: '#17232f',
  logo: 'none',
  logoData: '',
};

/** Aspecto neutro para QRs sin diseno guardado (no inventar estilos, REQ-MET-07). */
export const NEUTRAL_QR_DESIGN: QrDesign = {
  frame: 'none',
  frameText: '',
  frameTextColor: '#17232f',
  dotType: 'square',
  dotColor: '#17232f',
  background: '#ffffff',
  cornerType: 'square',
  cornerColor: '#17232f',
  cornerDotType: 'square',
  cornerDotColor: '#17232f',
  logo: 'none',
  logoData: '',
};

export const FRAME_OPTIONS: { value: QrFrameStyle; label: string }[] = [
  { value: 'none', label: 'Sin marco' },
  { value: 'text', label: 'Con texto' },
];

export const DOT_OPTIONS: { value: QrDotType; label: string }[] = [
  { value: 'square', label: 'Cuadrados' },
  { value: 'rounded', label: 'Redondos' },
  { value: 'classy', label: 'Classy' },
  { value: 'classy-rounded', label: 'Classy suave' },
  { value: 'dots', label: 'Puntos' },
];

export const CORNER_OPTIONS: { value: QrCornerType; label: string }[] = [
  { value: 'square', label: 'Cuadradas' },
  { value: 'dot', label: 'Punto' },
  { value: 'extra-rounded', label: 'Suaves' },
];

export const CORNER_DOT_OPTIONS: { value: QrCornerDotType; label: string }[] = [
  { value: 'square', label: 'Cuadrado' },
  { value: 'dot', label: 'Redondo' },
];

export const LOGO_OPTIONS: { value: QrLogoSource; label: string }[] = [
  { value: 'none', label: 'Sin logo' },
  { value: 'brand', label: 'Logo LibreQR' },
];

export function designToConfig(design: QrDesign): Record<string, unknown> {
  return { ...design };
}

const cache = new Map<string, QrDesign>();

/** Reconstruye el diseno desde un `designConfig` del backend (tolerante y con migracion). */
export function designFromConfig(config: unknown): QrDesign {
  if (!config) {
    return NEUTRAL_QR_DESIGN;
  }
  const raw = (typeof config === 'object' ? config : {}) as Record<string, unknown>;
  // el dataURL del logo puede ser grande: no lo metemos en la clave de cache
  const cacheKey = JSON.stringify({ ...raw, logoData: raw['logoData'] ? '!' : '' });
  const cached = cache.get(cacheKey);
  if (cached) {
    return cached;
  }
  const legacyCorner = legacyCornerType(raw['frame']);
  const design: QrDesign = {
    frame: isOneOf(FRAME_OPTIONS, raw['frame']) ? (raw['frame'] as QrFrameStyle) : 'none',
    frameText: typeof raw['frameText'] === 'string' ? (raw['frameText'] as string) : '',
    frameTextColor: hex(raw['frameTextColor'], raw['frameColor'], NEUTRAL_QR_DESIGN.frameTextColor),
    dotType: isOneOf(DOT_OPTIONS, raw['dotType']) ? (raw['dotType'] as QrDotType) : NEUTRAL_QR_DESIGN.dotType,
    dotColor: hex(raw['dotColor'], null, NEUTRAL_QR_DESIGN.dotColor),
    background: hex(raw['background'], null, NEUTRAL_QR_DESIGN.background),
    cornerType: isOneOf(CORNER_OPTIONS, raw['cornerType'])
      ? (raw['cornerType'] as QrCornerType)
      : legacyCorner,
    cornerColor: hex(raw['cornerColor'], raw['frameColor'], NEUTRAL_QR_DESIGN.cornerColor),
    cornerDotType: isOneOf(CORNER_DOT_OPTIONS, raw['cornerDotType'])
      ? (raw['cornerDotType'] as QrCornerDotType)
      : NEUTRAL_QR_DESIGN.cornerDotType,
    cornerDotColor: hex(raw['cornerDotColor'], raw['dotColor'], NEUTRAL_QR_DESIGN.cornerDotColor),
    logo: raw['logo'] === 'brand' || raw['logo'] === 'custom' ? (raw['logo'] as QrLogoSource) : 'none',
    logoData: typeof raw['logoData'] === 'string' ? (raw['logoData'] as string) : '',
  };
  cache.set(cacheKey, design);
  return design;
}

/** Configs antiguas: el "frame" controlaba las esquinas; ahora son conceptos aparte. */
function legacyCornerType(value: unknown): QrCornerType {
  switch (value) {
    case 'square':
      return 'square';
    case 'rounded':
    case 'extra-rounded':
      return 'extra-rounded';
    default:
      return 'square';
  }
}

function hex(value: unknown, fallbackValue: unknown, fallback: string): string {
  if (isHex(value)) {
    return value as string;
  }
  if (isHex(fallbackValue)) {
    return fallbackValue as string;
  }
  return fallback;
}

function isOneOf(options: { value: string }[], value: unknown): boolean {
  return typeof value === 'string' && options.some((option) => option.value === value);
}

function isHex(value: unknown): boolean {
  return typeof value === 'string' && /^#[0-9a-fA-F]{6}$/.test(value);
}
