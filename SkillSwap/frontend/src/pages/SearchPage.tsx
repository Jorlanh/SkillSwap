import { useEffect, useState } from 'react';
import { ChevronLeft, Search, LayoutGrid, List } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Link } from 'react-router-dom';
import { SearchService } from '@/services/api';

const filters = ['Popularidade', 'Comunidade', 'Pessoas', 'Personalizado', 'Palavra-chave'];

const SearchPage = () => {
  const [query, setQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('Popularidade');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    const normalizedQuery = query.trim();
    if (normalizedQuery.length < 2) {
      setSearchResults([]);
      setError(false);
      return;
    }

    const filter = activeFilter === 'Pessoas' ? 'USER' : activeFilter === 'Comunidade' ? 'COMMUNITY' : activeFilter === 'Palavra-chave' ? 'POST' : 'ALL';
    const timer = window.setTimeout(() => {
      setIsLoading(true);
      SearchService.search(normalizedQuery, filter)
        .then(({ data }) => setSearchResults(Array.isArray(data) ? data : []))
        .catch(() => { setSearchResults([]); setError(true); })
        .finally(() => setIsLoading(false));
    }, 250);

    return () => window.clearTimeout(timer);
  }, [query, activeFilter]);

  return (
    <div className="max-w-4xl mx-auto">
      <Link to="/" className="inline-flex items-center gap-1 text-foreground font-semibold mb-4 hover:opacity-80">
        <ChevronLeft className="h-5 w-5" /> Voltar
      </Link>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Filters */}
        <div className="w-full md:w-44 shrink-0">
          <h3 className="text-sm font-semibold text-foreground mb-3 flex items-center gap-1">
            <span>🔍</span> Filtros
          </h3>
          <div className="border-t border-border mb-3" />
          <div className="space-y-2">
            {filters.map((f) => (
              <button
                key={f}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  activeFilter === f
                    ? 'bg-skillswap-light-blue text-foreground'
                    : 'bg-muted text-muted-foreground hover:bg-accent'
                }`}
                onClick={() => setActiveFilter(f)}
              >
                {f}
                <span className="text-foreground">›</span>
              </button>
            ))}
          </div>
        </div>

        {/* Results */}
        <div className="flex-1">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-4">
            <div className="flex items-center gap-2">
              <Search className="h-5 w-5 text-foreground" />
              <h2 className="font-display font-bold text-xl text-foreground">Busca</h2>
            </div>
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="flex-1 bg-skillswap-card-bg border-none rounded-lg"
            />
          </div>
          <div className="border-t border-border mb-4" />

          <div className="flex justify-end gap-2 mb-4">
            <button className="p-1 text-muted-foreground hover:text-foreground">
              <LayoutGrid className="h-5 w-5" />
            </button>
            <button className="p-1 text-muted-foreground hover:text-foreground">
              <List className="h-5 w-5" />
            </button>
          </div>

          {isLoading ? <p className="py-8 text-center text-muted-foreground">Buscando...</p> : error ? <p className="py-8 text-center text-destructive">Não foi possível realizar a busca.</p> : searchResults.length === 0 ? <p className="py-8 text-center text-muted-foreground">Digite pelo menos duas letras para buscar no conteúdo da plataforma.</p> : <div className="space-y-3">
            {searchResults.map((result) => (
              <article key={`${result.type}-${result.id}`} className="rounded-xl border border-border bg-card p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">{result.type}</p>
                <h3 className="font-display font-bold text-foreground">{result.title}</h3>
                {result.description && <p className="mt-1 text-sm text-muted-foreground">{result.description}</p>}
              </article>
            ))}
          </div>}
        </div>
      </div>
    </div>
  );
};

export default SearchPage;
