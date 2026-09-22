import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  Signal,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import * as L from 'leaflet';
import 'leaflet.heat';

import { AuthService } from '../../core/auth/auth.service';
import { EleitorService } from '../eleitor/eleitor.service';
import { Perfil } from '../../shared/types/user';

interface NivelInicial {
  center: L.LatLngExpression;
  zoom: number;
  descricao: string;
}

// Aproximações hierárquicas por perfil. Skill 09/10 pode refinar quando o
// /me expuser 'cargo' do candidato (RF-04).
const NIVEIS: Record<Perfil, NivelInicial> = {
  SUPER_ADMIN_PLATAFORMA: { center: [-14.235, -51.9253], zoom: 4, descricao: 'Nacional' },
  ADMIN: { center: [-14.235, -51.9253], zoom: 4, descricao: 'Nacional (partido)' },
  CANDIDATO: { center: [-14.235, -51.9253], zoom: 5, descricao: 'Estado/Município (cargo)' },
  GERENTE_FINANCEIRO: { center: [-14.235, -51.9253], zoom: 4, descricao: 'Nacional' },
  SECRETARIO: { center: [-14.235, -51.9253], zoom: 4, descricao: 'Nacional' },
  LIDER_EQUIPE: { center: [-14.235, -51.9253], zoom: 10, descricao: 'Área da equipe' },
  MEMBRO_EQUIPE: { center: [-14.235, -51.9253], zoom: 12, descricao: 'Bairro/quadrante' },
};

/**
 * Mapa Leaflet com camada de heatmap dos eleitores cadastrados e markers
 * das equipes em tempo real (populados via DashboardComponent que compartilha
 * este componente).
 */
@Component({
  selector: 'sgce-mapa',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mapa-header">
      <strong>Mapa — {{ descricaoNivel() }}</strong>
      <small>{{ totalPontos() }} eleitores plotados</small>
    </div>
    <div #mapEl class="mapa"></div>
  `,
  styles: [
    `
      :host { display: block; height: 100%; }
      .mapa-header { display: flex; justify-content: space-between; padding: 6px 8px; background: #f1f5f9; border-radius: 4px 4px 0 0; }
      .mapa { height: 480px; width: 100%; border-radius: 0 0 6px 6px; overflow: hidden; }
    `,
  ],
})
export class MapaComponent implements AfterViewInit, OnDestroy {
  private auth = inject(AuthService);
  private eleitorSvc = inject(EleitorService);
  @ViewChild('mapEl', { static: true }) private mapEl!: ElementRef<HTMLDivElement>;

  private map?: L.Map;
  private heat?: L.HeatLayer;
  private markersMembros = new Map<number, L.CircleMarker>();

  private nivel: Signal<NivelInicial> = computed(() => {
    const p = this.auth.perfil();
    return p ? NIVEIS[p] : NIVEIS.MEMBRO_EQUIPE;
  });
  descricaoNivel = () => this.nivel().descricao;
  totalPontos = signal(0);

  async ngAfterViewInit(): Promise<void> {
    const nivel = this.nivel();
    this.map = L.map(this.mapEl.nativeElement).setView(nivel.center, nivel.zoom);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);

    await this.carregarHeatEleitores();
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  private async carregarHeatEleitores(): Promise<void> {
    try {
      const eleitores = await firstValueFrom(this.eleitorSvc.listar());
      const pontos: Array<[number, number, number]> = eleitores
        .filter(e => e.geolocalizacao)
        .map(e => [e.geolocalizacao!.latitude, e.geolocalizacao!.longitude, 0.6]);
      this.totalPontos.set(pontos.length);
      if (this.map && pontos.length) {
        this.heat?.remove();
        this.heat = L.heatLayer(pontos, {
          radius: 24,
          blur: 18,
          minOpacity: 0.35,
          gradient: { 0.2: '#dc2626', 0.5: '#eab308', 0.8: '#16a34a' },
        }).addTo(this.map);
        const bounds = L.latLngBounds(pontos.map(p => [p[0], p[1]] as L.LatLngExpression));
        this.map.fitBounds(bounds.pad(0.2));
      }
    } catch {
      /* sem dados ou sem permissão */
    }
  }

  /**
   * API pública para o Dashboard atualizar posição de membro em tempo real
   * conforme eventos heartbeat_membro chegam via STOMP.
   */
  atualizarMembro(membroId: number, lat: number, lng: number, corBorda = '#2563eb'): void {
    if (!this.map) return;
    const existente = this.markersMembros.get(membroId);
    if (existente) {
      existente.setLatLng([lat, lng]);
      existente.setStyle({ color: corBorda });
      return;
    }
    const marker = L.circleMarker([lat, lng], {
      radius: 8,
      color: corBorda,
      fillColor: '#3b82f6',
      fillOpacity: 0.8,
      weight: 2,
    })
      .bindTooltip(`Membro #${membroId}`, { permanent: false })
      .addTo(this.map);
    this.markersMembros.set(membroId, marker);
  }

  marcarMembroOffline(membroId: number): void {
    const m = this.markersMembros.get(membroId);
    if (m) {
      m.setStyle({ color: '#f59e0b', fillColor: '#fde68a' });
      m.bindTooltip(`Membro #${membroId} — sem conexão`, { permanent: false });
    }
  }
}
