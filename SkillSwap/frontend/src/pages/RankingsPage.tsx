import { useEffect, useState } from 'react';
import { RankingService } from '@/services/api';
import VerifiedBadge from '@/components/VerifiedBadge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Trophy, Star } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';

const RankingsPage = () => {
  const [rankings, setRankings] = useState<any[]>([]);
  const [skills, setSkills] = useState<any[]>([]);
  const [selectedSkill, setSelectedSkill] = useState<string>('Ranking Geral');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Carrega habilidades do backend com tratamento de erro
    RankingService.getSkills()
      .then(res => setSkills(Array.isArray(res.data) ? res.data : []))
      .catch((err) => {
        console.error("Falha ao carregar lista de habilidades:", err);
        setSkills([]);
      });
  }, []);

  useEffect(() => {
    const loadRankings = async () => {
      setIsLoading(true);
      try {
        const { data } = selectedSkill === 'Ranking Geral' 
          ? await RankingService.getAll() 
          : await RankingService.getBySkill(selectedSkill);
        setRankings(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error("Erro na telemetria de rankings.");
        setRankings([]);
      } finally {
        setIsLoading(false);
      }
    };
    loadRankings();
  }, [selectedSkill]);

  return (
    <div className="max-w-3xl mx-auto p-4">
      <div className="flex items-center justify-between mb-8">
        <h1 className="font-display font-bold text-2xl text-foreground flex items-center gap-2">
          <Trophy className="h-6 w-6 text-yellow-500" /> Rankings Global
        </h1>
        
        <Select value={selectedSkill} onValueChange={setSelectedSkill}>
          <SelectTrigger className="w-56 bg-card border-skillswap-teal/20">
            <SelectValue placeholder="Ranking Geral" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="Ranking Geral">Ranking Geral</SelectItem>
            {skills.map((skill: any) => {
              // MANOBRA DE SEGURANÇA: O valor não pode ser string vazia ("")
              // Se o id não existir, usamos um fallback temporário para evitar crash no Radix
              const skillValue = skill?.id ? skill.id.toString() : `temp-${Math.random()}`;
              
              return (
                <SelectItem 
                  key={skill?.id || Math.random()} 
                  value={skillValue}
                >
                  {skill?.name || "Habilidade sem nome"}
                </SelectItem>
              );
            })}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-3">
        {isLoading ? (
          Array(5).fill(0).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)
        ) : rankings.length > 0 ? (
          rankings.map((user, index) => (
            <div
              key={user?.id || index}
              className={`flex items-center gap-4 rounded-2xl px-6 py-4 border border-border transition-all hover:scale-[1.01] ${
                index < 3 ? 'bg-skillswap-teal/5 border-skillswap-teal/30 shadow-sm' : 'bg-card'
              }`}
            >
              <span className={`font-display font-black text-xl w-8 text-center ${
                index === 0 ? 'text-yellow-500' : index === 1 ? 'text-zinc-400' : index === 2 ? 'text-amber-700' : 'text-muted-foreground'
              }`}>
                {index + 1}º
              </span>

              <Avatar className="h-12 w-12 ring-2 ring-background shadow-sm">
                <AvatarImage src={user?.avatarUrl} />
                <AvatarFallback>{user?.username?.[0] || '?'}</AvatarFallback>
              </Avatar>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1.5">
                  <h3 className="font-display font-bold text-foreground truncate text-lg">{user?.username || "Usuário"}</h3>
                  <VerifiedBadge user={user} size={18} />
                </div>
                <div className="flex items-center gap-4 mt-1">
                  <div className="flex items-center gap-1 text-xs font-bold text-yellow-500 bg-yellow-500/10 px-2 py-0.5 rounded-full">
                    <Star size={10} fill="currentColor" /> {user?.averageRating?.toFixed(1) || "5.0"}
                  </div>
                  <span className="text-[10px] uppercase font-black text-muted-foreground tracking-tighter">
                    {user?.totalSwaps || 0} trocas concluídas
                  </span>
                </div>
              </div>

              <div className="text-right">
                <p className="text-[10px] font-black uppercase text-muted-foreground">Score</p>
                <p className="text-xl font-black text-skillswap-teal">
                  {user?.rankingPoints || ((user?.totalSwaps || 0) * 10)}
                </p>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-20 text-muted-foreground">
             Nenhum mestre encontrado para esta categoria ainda.
          </div>
        )}
      </div>
    </div>
  );
};

export default RankingsPage;