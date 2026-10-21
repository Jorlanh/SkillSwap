import { mockCommunities } from '@/data/mockData';
import CommunityCard from '@/components/CommunityCard';
import { ChevronLeft, PlusCircle } from 'lucide-react';
import { Link, useOutletContext } from 'react-router-dom';
import { Button } from '@/components/ui/button';

const CommunitiesPage = () => {
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-skillswap-card-bg rounded-2xl p-6">
        <div className="flex items-center justify-between mb-2">
          <Link to="/" className="inline-flex items-center gap-1 text-foreground font-semibold hover:opacity-80">
            <ChevronLeft className="h-5 w-5" /> Voltar
          </Link>
          <Button 
            variant="outline" 
            className="flex items-center gap-2"
            onClick={() => requireAuth(() => console.log("Explorar mais comunidades"))}
          >
            Explorar mais <PlusCircle className="h-4 w-4" />
          </Button>
        </div>

        <h1 className="font-display font-bold text-3xl text-foreground text-center mb-1">Comunidade</h1>
        <p className="text-center text-muted-foreground mb-8">Venha fazer parte de alguma também! :)</p>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {mockCommunities.map((community) => (
            <CommunityCard 
              key={community.id} 
              community={community} 
              onJoin={() => console.log(`Entrou na: ${community.name}`)}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default CommunitiesPage;