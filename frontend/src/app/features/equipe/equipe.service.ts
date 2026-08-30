import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Equipe {
  id: number;
  partidoId: number;
  nome: string;
  liderId: number;
  regiaoAtuacao?: string;
  criadoEm: string;
}

export interface MembroEquipe {
  id: number;
  equipeId: number;
  usuarioId: number;
  funcao?: string;
  ativo: boolean;
}

export interface EquipeCandidato {
  id: number;
  equipeId: number;
  candidatoId: number;
  vigenteDesde: string;
  vigenteAte?: string | null;
}

@Injectable({ providedIn: 'root' })
export class EquipeService {
  private http = inject(HttpClient);

  listar(): Observable<Equipe[]> {
    return this.http.get<Equipe[]>('/api/equipes');
  }
  criar(input: { partidoId?: number; nome: string; liderId: number; regiaoAtuacao?: string }): Observable<Equipe> {
    return this.http.post<Equipe>('/api/equipes', input);
  }
  adicionarMembro(equipeId: number, input: { usuarioId: number; funcao?: string }): Observable<MembroEquipe> {
    return this.http.post<MembroEquipe>(`/api/equipes/${equipeId}/membros`, input);
  }
  listarMembros(equipeId: number): Observable<MembroEquipe[]> {
    return this.http.get<MembroEquipe[]>(`/api/equipes/${equipeId}/membros`);
  }
  vincularCandidato(equipeId: number, input: { candidatoId: number; vigenteDesde: string; vigenteAte?: string }): Observable<EquipeCandidato> {
    return this.http.post<EquipeCandidato>(`/api/equipes/${equipeId}/candidatos`, input);
  }
}
