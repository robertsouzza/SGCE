import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Partido {
  id: number;
  nome: string;
  sigla: string;
  numeroPartido: number;
  cnpj: string;
  telefone?: string;
  planoAssinatura: string;
  ativo: boolean;
  criadoEm: string;
}

export type Cargo =
  | 'PRESIDENTE'
  | 'GOVERNADOR'
  | 'SENADOR'
  | 'DEPUTADO_FEDERAL'
  | 'DEPUTADO_ESTADUAL'
  | 'PREFEITO'
  | 'VEREADOR';

export const CARGOS: Array<{ valor: Cargo; rotulo: string }> = [
  { valor: 'PRESIDENTE', rotulo: 'Presidente' },
  { valor: 'GOVERNADOR', rotulo: 'Governador' },
  { valor: 'SENADOR', rotulo: 'Senador' },
  { valor: 'DEPUTADO_FEDERAL', rotulo: 'Deputado Federal' },
  { valor: 'DEPUTADO_ESTADUAL', rotulo: 'Deputado Estadual' },
  { valor: 'PREFEITO', rotulo: 'Prefeito' },
  { valor: 'VEREADOR', rotulo: 'Vereador' },
];

export interface UF {
  sigla: string;
  nome: string;
}

export const UFS: UF[] = [
  { sigla: 'AC', nome: 'Acre' },
  { sigla: 'AL', nome: 'Alagoas' },
  { sigla: 'AP', nome: 'Amapá' },
  { sigla: 'AM', nome: 'Amazonas' },
  { sigla: 'BA', nome: 'Bahia' },
  { sigla: 'CE', nome: 'Ceará' },
  { sigla: 'DF', nome: 'Distrito Federal' },
  { sigla: 'ES', nome: 'Espírito Santo' },
  { sigla: 'GO', nome: 'Goiás' },
  { sigla: 'MA', nome: 'Maranhão' },
  { sigla: 'MT', nome: 'Mato Grosso' },
  { sigla: 'MS', nome: 'Mato Grosso do Sul' },
  { sigla: 'MG', nome: 'Minas Gerais' },
  { sigla: 'PA', nome: 'Pará' },
  { sigla: 'PB', nome: 'Paraíba' },
  { sigla: 'PR', nome: 'Paraná' },
  { sigla: 'PE', nome: 'Pernambuco' },
  { sigla: 'PI', nome: 'Piauí' },
  { sigla: 'RJ', nome: 'Rio de Janeiro' },
  { sigla: 'RN', nome: 'Rio Grande do Norte' },
  { sigla: 'RS', nome: 'Rio Grande do Sul' },
  { sigla: 'RO', nome: 'Rondônia' },
  { sigla: 'RR', nome: 'Roraima' },
  { sigla: 'SC', nome: 'Santa Catarina' },
  { sigla: 'SP', nome: 'São Paulo' },
  { sigla: 'SE', nome: 'Sergipe' },
  { sigla: 'TO', nome: 'Tocantins' },
];

export interface Candidato {
  id: number;
  partidoId: number;
  nomeCompleto: string;
  tituloEleitor: string;
  numeroCandidato: number;
  cargo: Cargo;
  uf: string;
  municipio?: string;
  criadoEm: string;
}

export interface CriarPartidoInput {
  nome: string;
  sigla: string;
  numeroPartido: number;
  cnpj: string;
  telefone?: string;
}

export interface CriarCandidatoInput {
  partidoId: number;
  nomeCompleto: string;
  tituloEleitor: string;
  numeroCandidato: number;
  cargo: Cargo;
  uf: string;
  municipio?: string;
}

@Injectable({ providedIn: 'root' })
export class PartidoService {
  private http = inject(HttpClient);

  listar(): Observable<Partido[]> {
    return this.http.get<Partido[]>('/api/partidos');
  }
  criar(input: CriarPartidoInput): Observable<Partido> {
    return this.http.post<Partido>('/api/partidos', input);
  }

  listarCandidatos(): Observable<Candidato[]> {
    return this.http.get<Candidato[]>('/api/candidatos');
  }
  criarCandidato(input: CriarCandidatoInput): Observable<Candidato> {
    return this.http.post<Candidato>('/api/candidatos', input);
  }

  static exigeMunicipio(cargo: Cargo): boolean {
    return cargo === 'PREFEITO' || cargo === 'VEREADOR';
  }
}
