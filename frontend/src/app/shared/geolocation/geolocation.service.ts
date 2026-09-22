import { Injectable } from '@angular/core';

export interface GeoPoint {
  longitude: number;
  latitude: number;
}

/**
 * Wrapper de navigator.geolocation com Promise + fallback silencioso.
 * Retorna null se a API não estiver disponível OU se o usuário negar acesso.
 */
@Injectable({ providedIn: 'root' })
export class GeolocationService {
  async obterPosicao(timeoutMs = 5000): Promise<GeoPoint | null> {
    if (!('geolocation' in navigator)) return null;
    return new Promise(resolve => {
      navigator.geolocation.getCurrentPosition(
        pos => resolve({ longitude: pos.coords.longitude, latitude: pos.coords.latitude }),
        () => resolve(null),
        { enableHighAccuracy: true, timeout: timeoutMs, maximumAge: 30_000 },
      );
    });
  }

  observarPosicao(cb: (p: GeoPoint) => void): number | null {
    if (!('geolocation' in navigator)) return null;
    return navigator.geolocation.watchPosition(
      pos => cb({ longitude: pos.coords.longitude, latitude: pos.coords.latitude }),
      () => {
        /* silencioso */
      },
      { enableHighAccuracy: true, maximumAge: 15_000 },
    );
  }

  pararObservacao(id: number | null): void {
    if (id != null && 'geolocation' in navigator) navigator.geolocation.clearWatch(id);
  }
}
