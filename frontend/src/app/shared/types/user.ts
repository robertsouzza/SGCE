export type Perfil =
  | 'SUPER_ADMIN_PLATAFORMA'
  | 'ADMIN'
  | 'CANDIDATO'
  | 'GERENTE_FINANCEIRO'
  | 'SECRETARIO'
  | 'LIDER_EQUIPE'
  | 'MEMBRO_EQUIPE';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  partidoId: number | '' | null;
}
