import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GeoPoint } from '../../shared/geolocation/geolocation.service';

export interface Eleitor {
  id: number;
  nomeCompleto: string;
  endereco: string | null;
  telefoneWhatsapp: string | null;
  tituloEleitor: string;
  zonaEleitoral: string | null;
  secaoEleitoral: string | null;
  observacoes: string | null;
  geolocalizacao: GeoPoint | null;
  anonimizado?: boolean;
}

export interface CadastrarEleitorPayload {
  nomeCompleto: string;
  endereco?: string | null;
  geolocalizacao?: GeoPoint | null;
  telefoneWhatsapp?: string | null;
  tituloEleitor: string;
  zonaEleitoral?: string | null;
  secaoEleitoral?: string | null;
  observacoes?: string | null;
}

export type Intencao = 'FAVORAVEL' | 'INDECISO' | 'CONTRARIO' | 'HOSTIL';
export type TipoAbordagem = 'DOMICILIAR' | 'PUBLICA';

export interface RegistrarAbordagemPayload {
  eleitorId: number;
  equipeId?: number | null;
  tipoAbordagem: TipoAbordagem;
  geolocalizacao?: GeoPoint | null;
  timestampLocal?: string;
  intencoes: Array<{ candidatoId: number; intencao: Intencao }>;
}

export interface Abordagem {
  id: number;
  eleitorId: number;
  equipeId: number | null;
  tipoAbordagem: TipoAbordagem;
  dataHora: string;
  intencoes: Array<{ candidatoId: number; intencao: Intencao }>;
}

export type MetodoCaptura = 'ASSINATURA_TELA' | 'DEEP_LINK_WHATSAPP';

export interface CapturarConsentimentoPayload {
  eleitorId: number;
  abordagemId: number | null;
  metodoCaptura: MetodoCaptura;
  geolocalizacao?: GeoPoint | null;
  timestampLocal?: string;
  consentimentoDados: boolean;
  consentimentoWhatsappMarketing: boolean;
}

export interface ConsentimentoLGPD {
  id: number;
  eleitorId: number;
  metodoCaptura: MetodoCaptura;
  consentimentoDados: boolean;
  consentimentoWhatsappMarketing: boolean;
  dataCaptura: string;
  dataRevogacaoDados: string | null;
  dataRevogacaoWhatsapp: string | null;
  urlAssinatura: string | null;
}

export interface DeepLinkOptIn {
  url: string;
  qrCodePngDataUri: string;
  optInId: string;
}

@Injectable({ providedIn: 'root' })
export class EleitorService {
  private http = inject(HttpClient);

  listar(): Observable<Eleitor[]> {
    return this.http.get<Eleitor[]>('/api/eleitores');
  }

  buscar(id: number): Observable<Eleitor> {
    return this.http.get<Eleitor>(`/api/eleitores/${id}`);
  }

  cadastrar(payload: CadastrarEleitorPayload): Observable<Eleitor> {
    return this.http.post<Eleitor>('/api/eleitores', payload);
  }

  registrarAbordagem(payload: RegistrarAbordagemPayload): Observable<Abordagem> {
    return this.http.post<Abordagem>('/api/abordagens', payload);
  }

  capturarConsentimento(payload: CapturarConsentimentoPayload): Observable<ConsentimentoLGPD> {
    return this.http.post<ConsentimentoLGPD>('/api/consentimentos', payload);
  }

  anexarAssinatura(consentimentoId: number, pngDataUri: string): Observable<ConsentimentoLGPD> {
    const blob = dataUriParaBlob(pngDataUri);
    const form = new FormData();
    form.append('arquivo', blob, `assinatura-${consentimentoId}.png`);
    return this.http.post<ConsentimentoLGPD>(
      `/api/consentimentos/${consentimentoId}/assinatura`,
      form,
    );
  }

  revogarDados(consentimentoId: number): Observable<ConsentimentoLGPD> {
    return this.http.post<ConsentimentoLGPD>(
      `/api/consentimentos/${consentimentoId}/revogar-dados`,
      {},
    );
  }

  anonimizar(eleitorId: number): Observable<Eleitor> {
    return this.http.post<Eleitor>(`/api/eleitores/${eleitorId}/anonimizar`, {});
  }

  gerarDeepLink(abordagemId: number, candidatoId: number): Observable<DeepLinkOptIn> {
    return this.http.get<DeepLinkOptIn>('/api/consentimentos/deep-link-opt-in', {
      params: { abordagemId, candidatoId },
    });
  }
}

function dataUriParaBlob(dataUri: string): Blob {
  const [meta, base64] = dataUri.split(',');
  const mime = /data:(.*?);base64/.exec(meta)?.[1] ?? 'image/png';
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return new Blob([bytes], { type: mime });
}
