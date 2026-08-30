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

export type Cargo = 'PRESIDENTE' | 'SENADOR' | 'DEPUTADO_FEDERAL' | 'DEPUTADO_ESTADUAL' | 'PREFEITO' | 'VEREADOR';

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
