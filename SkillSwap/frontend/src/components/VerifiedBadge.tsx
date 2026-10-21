import verifiedImg from '/SKILLSWAP VERIFICADO.png';

interface VerifiedBadgeProps {
  user?: { // Tornado opcional para evitar crash
    isVerified?: boolean;
    completedSwaps?: number;
  } | null;
  className?: string;
  size?: number;
}

const VerifiedBadge = ({ user, className = '', size = 18 }: VerifiedBadgeProps) => {
  // Blindagem contra null/undefined
  if (!user) return null;

  // Regra de negócio com fallback para falso/zero
  const isEligible = (user.isVerified || (user.completedSwaps ?? 0) >= 1000);

  if (!isEligible) return null;

  return (
    <img
      src={verifiedImg}
      alt="Selo Verificado"
      width={size}
      height={size}
      className={`inline-block ml-1 align-center ${className}`}
      loading="lazy"
    />
  );
};

export default VerifiedBadge;