import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AdminService } from '@/services/api';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ShieldCheck, UserX, UserCheck, Activity, Users, Zap, ArrowLeft, KeyRound } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

type Stats = { totalUsers: number; totalSkills: number; totalProposals: number; pendingProposals: number; completedProposals: number };
type ManagedUser = {
  userId: string;
  username: string;
  name: string;
  email: string;
  verifiedBadge: boolean;
  roles: string[];
  banned: boolean;
  createdAt: string;
};

const AdminConsole = () => {
  const [stats, setStats] = useState<Stats | null>(null);
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes] = await Promise.all([
        AdminService.getStats(),
        AdminService.getUsers(),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data);
    } catch {
      toast({ title: 'Acesso negado', description: 'Sua conta não possui a função ADMIN.', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { loadAdminData(); }, [loadAdminData]);

  const handleToggleVerify = async (userId: string) => {
    try {
      await AdminService.toggleVerification(userId);
      toast({ title: 'Selo atualizado', description: 'O status de verificação foi alterado.' });
      loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível alterar o selo.', variant: 'destructive' });
    }
  };

  const handleBan = async (userId: string, banned: boolean) => {
    try {
      if (banned) {
        await AdminService.unbanUser(userId);
      } else {
        await AdminService.banUser(userId, { reason: 'Ação administrativa pela plataforma', expiresAt: null, ipAddress: null });
      }
      toast({ title: banned ? 'Usuário reativado' : 'Usuário banido' });
      loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível atualizar o banimento.', variant: 'destructive' });
    }
  };

  const handleRole = async (user: ManagedUser) => {
    const enabled = !user.roles.some((role) => role.toUpperCase() === 'ADMIN');
    try {
      await AdminService.setRole(user.userId, 'ADMIN', enabled);
      toast({ title: enabled ? 'Administrador adicionado' : 'Administrador removido' });
      loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível atualizar a função.', variant: 'destructive' });
    }
  };

  return (
    <div className="p-8 space-y-8 bg-zinc-950 min-h-screen text-white">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-red-400 font-black">Área protegida</p>
          <h1 className="text-3xl font-bold flex items-center gap-3"><ShieldCheck className="text-red-500" size={32} /> Painel do administrador</h1>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant="outline" className="text-red-500 border-red-500">ADMIN</Badge>
          <Link to="/" className="inline-flex items-center gap-2 text-sm text-zinc-300 hover:text-white"><ArrowLeft size={16} /> Voltar ao usuário</Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <StatCard title="Usuários" value={stats?.totalUsers ?? 0} icon={<Users />} />
        <StatCard title="Propostas" value={stats?.totalProposals ?? 0} icon={<Activity />} />
        <StatCard title="Pendentes" value={stats?.pendingProposals ?? 0} icon={<Zap />} />
        <StatCard title="Concluídas" value={stats?.completedProposals ?? 0} icon={<KeyRound />} />
      </div>

      <Card className="bg-zinc-900 border-zinc-800 text-white">
        <CardHeader><CardTitle>Gestão de usuários, selos e segurança</CardTitle></CardHeader>
        <CardContent>
          {loading ? <p className="text-zinc-400 py-8">Carregando dados protegidos...</p> : (
            <Table>
              <TableHeader><TableRow className="border-zinc-800"><TableHead className="text-zinc-400">Usuário</TableHead><TableHead className="text-zinc-400">Papéis</TableHead><TableHead className="text-zinc-400">Verificação</TableHead><TableHead className="text-zinc-400 text-right">Ações</TableHead></TableRow></TableHeader>
              <TableBody>{users.map((user) => {
                const isAdmin = user.roles.some((role) => role.toUpperCase() === 'ADMIN');
                return <TableRow key={user.userId} className="border-zinc-800">
                  <TableCell><p className="font-medium">{user.name || user.username}</p><p className="text-xs text-zinc-500">{user.email}</p></TableCell>
                  <TableCell><div className="flex gap-1 flex-wrap">{user.roles.map((role) => <Badge key={role} variant="outline">{role}</Badge>)}</div></TableCell>
                  <TableCell>{user.verifiedBadge ? <Badge className="bg-green-600">Verificado</Badge> : <Badge variant="outline">Pendente</Badge>}</TableCell>
                  <TableCell className="text-right space-x-2 space-y-2">
                    <Button size="sm" variant="outline" onClick={() => handleToggleVerify(user.userId)}>{user.verifiedBadge ? 'Remover selo' : 'Dar selo'}</Button>
                    <Button size="sm" variant={isAdmin ? 'destructive' : 'outline'} onClick={() => handleRole(user)}><KeyRound size={14} className="mr-1" />{isAdmin ? 'Remover admin' : 'Tornar admin'}</Button>
                    {user.banned ? <Button size="sm" variant="outline" onClick={() => handleBan(user.userId, true)}><UserCheck size={14} className="mr-1" />Reativar</Button> : <Button size="sm" variant="destructive" onClick={() => handleBan(user.userId, false)}><UserX size={14} className="mr-1" />Banir</Button>}
                  </TableCell>
                </TableRow>;
              })}</TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

const StatCard = ({ title, value, icon }: { title: string; value: number | string; icon: React.ReactNode }) => (
  <Card className="bg-zinc-900 border-zinc-800 text-white"><CardHeader className="flex flex-row items-center justify-between pb-2"><CardTitle className="text-sm font-medium text-zinc-400">{title}</CardTitle><div className="text-zinc-500">{icon}</div></CardHeader><CardContent><div className="text-2xl font-bold">{value}</div></CardContent></Card>
);

export default AdminConsole;