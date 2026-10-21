import { Badge } from '@/components/ui/badge';

interface SkillBadgeProps {
  skill: string;
}

const SkillBadge = ({ skill }: SkillBadgeProps) => {
  return (
    <Badge variant="outline" className="bg-muted text-foreground border-border px-3 py-1 text-xs font-medium">
      {skill}
    </Badge>
  );
};

export default SkillBadge;
