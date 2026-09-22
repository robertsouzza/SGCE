import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { GeolocationService } from '../../shared/geolocation/geolocation.service';

/**
 * Toggle "modo campo" (D-10). Quando ativo, dispara POST /api/tempo-real/heartbeat
 * a cada 30s com a geoloc atual. Backend só aceita para MEMBRO/LIDER com
 * ConsentimentoMembro ativo — o toggle deste service NÃO faz a verificação
 * de consentimento localmente (o backend responderia 403); ao receber 403,
 * o toggle desliga sozinho e sinaliza erro em `ultimoErro()`.
 */
@Injectable({ providedIn: 'root' })
export class ModoCampoService {
  private http = inject(HttpClient);
  private geo = inject(GeolocationService);

  readonly ativo = signal(false);
  readonly ultimoErro = signal<string | null>(null);
  readonly ultimoEnvio = signal<string | null>(null);
  private timer?: ReturnType<typeof setInterval>;

  private static readonly INTERVALO_MS = 30_000;

  async ligar(): Promise<void> {
    if (this.ativo()) return;
    this.ultimoErro.set(null);
    const ok = await this.enviarHeartbeat();
    if (!ok) return; // erro já registrado; toggle não liga
    this.ativo.set(true);
    this.timer = setInterval(() => this.enviarHeartbeat(), ModoCampoService.INTERVALO_MS);
  }

  desligar(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = undefined;
    this.ativo.set(false);
  }

  private async enviarHeartbeat(): Promise<boolean> {
    const g = await this.geo.obterPosicao(3000);
    if (!g) {
      this.ultimoErro.set('Geolocalização indisponível');
      return false;
    }
    try {
      await firstValueFrom(
        this.http.post('/api/tempo-real/heartbeat', {
          geolocalizacao: { longitude: g.longitude, latitude: g.latitude },
          statusConexao: navigator.onLine ? 'ONLINE' : 'OFFLINE_COLETANDO',
        }),
      );
      this.ultimoEnvio.set(new Date().toISOString());
      this.ultimoErro.set(null);
      return true;
    } catch (e: unknown) {
      const status = (e as { status?: number })?.status;
      if (status === 403) {
        this.ultimoErro.set(
          'Você precisa aceitar o termo de consentimento de voluntário antes de ativar o modo campo.',
        );
      } else {
        this.ultimoErro.set('Falha ao enviar heartbeat: ' + status);
      }
      this.desligar();
      return false;
    }
  }
}
