import { suggestedCommunities, trendingTopics } from '@/data/mockData';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';

const RightSidebar = () => {
  return (
    <aside className="hidden xl:block w-64 shrink-0 space-y-6">
      {/* Communities */}
      <div className="bg-card rounded-xl border border-border p-4">
        <h3 className="font-display font-semibold text-foreground mb-3">Comunidades para você</h3>
        <div className="space-y-3">
          {suggestedCommunities.map((c) => (
            <div key={c.name} className="flex items-center gap-2">
              <Avatar className="h-8 w-8">
                <AvatarImage src={c.avatar} />
                <AvatarFallback>C</AvatarFallback>
              </Avatar>
              <span className="text-sm text-foreground flex-1">{c.name}</span>
              <Button size="sm" className="bg-skillswap-teal text-primary-foreground text-xs h-6 px-2 hover:opacity-90">
                Juntar-se
              </Button>
            </div>
          ))}
        </div>
        <button className="text-sm text-primary mt-3 hover:underline">Mostrar mais</button>
      </div>

      {/* Topics */}
      <div>
        <h3 className="font-display font-semibold text-foreground mb-3">Assuntos</h3>
        <div className="space-y-1">
          {trendingTopics.map((t) => (
            <button key={t} className="block text-sm text-muted-foreground hover:text-foreground transition-colors">
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* Resources */}
      <div>
        <h3 className="font-display font-semibold text-foreground mb-3">Recursos</h3>
        <div className="space-y-1">
          {['Sobre o SkillSwap', 'Anuncie aqui', 'Ajuda', 'Notícias', 'Blog'].map((r) => (
            <button key={r} className="block text-sm text-muted-foreground hover:text-foreground transition-colors">
              {r}
            </button>
          ))}
        </div>
      </div>

      <div className="text-xs text-muted-foreground space-y-1">
        <p>Regras do SkillSwap | Políticas de Privacidade</p>
        <p>Contrato de Usuário | Termos de Serviço</p>
        <p>SkillSwap, Inc. © 2025</p>
      </div>
    </aside>
  );
};

export default RightSidebar;
