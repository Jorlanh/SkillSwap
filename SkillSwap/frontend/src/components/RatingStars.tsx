import { Star } from 'lucide-react';
import { useState } from 'react';

interface RatingStarsProps {
  value: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
  size?: number;
}

const RatingStars = ({ value, onChange, readOnly = false, size = 24 }: RatingStarsProps) => {
  const [hover, setHover] = useState(0);

  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={readOnly}
          className={`transition-colors ${readOnly ? 'cursor-default' : 'cursor-pointer'}`}
          onClick={() => onChange?.(star)}
          onMouseEnter={() => !readOnly && setHover(star)}
          onMouseLeave={() => !readOnly && setHover(0)}
        >
          <Star
            className={`${
              star <= (hover || value)
                ? 'text-skillswap-gold fill-skillswap-gold'
                : 'text-muted-foreground'
            }`}
            style={{ width: size, height: size }}
          />
        </button>
      ))}
    </div>
  );
};

export default RatingStars;
