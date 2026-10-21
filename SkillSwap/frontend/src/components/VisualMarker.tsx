export type VisualMarkerKind = 'BLUE_TRIANGLE' | 'ORANGE_SQUARE' | 'NEUTRAL_CIRCLE';

export const VISUAL_MARKERS: Array<{
  value: VisualMarkerKind;
  label: string;
  description: string;
}> = [
  {
    value: 'BLUE_TRIANGLE',
    label: 'Azul com triângulo',
    description: 'Marcador visual azul com triângulo, escolhido pelo usuário',
  },
  {
    value: 'ORANGE_SQUARE',
    label: 'Laranja com quadrado',
    description: 'Marcador visual laranja com quadrado, escolhido pelo usuário',
  },
  {
    value: 'NEUTRAL_CIRCLE',
    label: 'Branco com círculo',
    description: 'Marcador visual branco com círculo, escolhido pelo usuário',
  },
];

interface VisualMarkerProps {
  marker?: string | null;
  size?: number;
  showLabel?: boolean;
  className?: string;
}

export const normalizeVisualMarker = (marker?: string | null): VisualMarkerKind => {
  if (marker === 'BLUE_TRIANGLE' || marker === 'ORANGE_SQUARE' || marker === 'NEUTRAL_CIRCLE') {
    return marker;
  }
  return 'NEUTRAL_CIRCLE';
};

const VisualMarker = ({ marker, size = 18, showLabel = false, className = '' }: VisualMarkerProps) => {
  const normalized = normalizeVisualMarker(marker);
  const metadata = VISUAL_MARKERS.find((item) => item.value === normalized) ?? VISUAL_MARKERS[2];
  const shape = normalized === 'BLUE_TRIANGLE' ? 'triangle' : normalized === 'ORANGE_SQUARE' ? 'square' : 'circle';

  return (
    <span
      className={`visual-marker visual-marker-${normalized.toLowerCase()} ${className}`}
      title={metadata.description}
      aria-label={metadata.description}
      role="img"
    >
      <span
        aria-hidden="true"
        className={`visual-marker-shape visual-marker-shape-${shape}`}
        style={{ width: size, height: size }}
      />
      {showLabel && <span className="text-xs font-semibold">{metadata.label}</span>}
      {!showLabel && <span className="sr-only">{metadata.description}</span>}
    </span>
  );
};

export default VisualMarker;
