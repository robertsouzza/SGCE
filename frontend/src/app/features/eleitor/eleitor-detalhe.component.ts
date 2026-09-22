import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { GeolocationService } from '../../shared/geolocation/geolocation.service';
import { SignaturePadComponent } from '../../shared/signature-pad/signature-pad.component';
import { EquipeService } from '../equipe/equipe.service';
import {
  Eleitor,
  EleitorService,
  Intencao,
  TipoAbordagem,
  DeepLinkOptIn,
} from './eleitor.service';

/**
 * Detalhe do eleitor: mostra dados + botões de revogar + form de abordagem
 * (com intenção múltipla) e captura de consentimento em 2 abas — assinatura
 * em tela OU deep-link wa.me via QR (D-01, RF-14, RF-15).
 */
@Component({
  selector: 'sgce-eleitor-detalhe',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SignaturePadComponent],
  template: `
    <div class="page">
      <a routerLink="/eleitores">&larr; Voltar</a>
      @if (eleitor(); as e) {
        <h2>{{ e.anonimizado ? 'Eleitor anonimizado #' + e.id : e.nomeCompleto }}</h2>
        <dl class="dados">
          <dt>Título</dt><dd>{{ e.tituloEleitor }}</dd>
          <dt>WhatsApp</dt><dd>{{ e.telefoneWhatsapp || '—' }}</dd>
          <dt>Endereço</dt><dd>{{ e.endereco || '—' }}</dd>
          <dt>Zona/Seção</dt><dd>{{ e.zonaEleitoral || '—' }} / {{ e.secaoEleitoral || '—' }}</dd>
        </dl>

        <section>
          <h3>Nova abordagem</h3>
          <label>
            Tipo
            <select [(ngModel)]="tipo">
              <option value="DOMICILIAR">Domiciliar</option>
              <option value="PUBLICA">Pública</option>
            </select>
          </label>
          <label>
            Equipe
            <select [(ngModel)]="equipeIdSel" (change)="carregarCandidatos()">
              <option [ngValue]="null">— selecione —</option>
              @for (eq of equipes(); track eq.id) {
                <option [ngValue]="eq.id">{{ eq.nome }}</option>
              }
            </select>
          </label>

          @if (candidatosVinculados().length) {
            <div class="intencoes">
              <strong>Intenção por candidato:</strong>
              @for (c of candidatosVinculados(); track c.candidatoId) {
                <div class="linha-intencao">
                  <span>Candidato #{{ c.candidatoId }}</span>
                  @for (op of opcoesIntencao; track op) {
                    <label>
                      <input type="radio"
                             [name]="'int-' + c.candidatoId"
                             [value]="op"
                             [(ngModel)]="intencoesMap[c.candidatoId]" />
                      {{ op }}
                    </label>
                  }
                </div>
              }
            </div>
          }

          <button class="primary"
                  [disabled]="!temIntencaoValida() || registrando()"
                  (click)="registrarAbordagem()">
            {{ registrando() ? 'Registrando...' : 'Registrar abordagem' }}
          </button>
          @if (abordagemAtualId()) {
            <p class="ok">Abordagem #{{ abordagemAtualId() }} criada. Capture o consentimento abaixo.</p>
          }
        </section>

        @if (abordagemAtualId()) {
          <section>
            <h3>Consentimento LGPD</h3>
            <div class="abas">
              <button [class.ativo]="abaConsentimento() === 'assinatura'"
                      (click)="abaConsentimento.set('assinatura')">
                Assinatura em tela
              </button>
              <button [class.ativo]="abaConsentimento() === 'qr'"
                      (click)="abaConsentimento.set('qr'); solicitarDeepLink()">
                QR wa.me
              </button>
            </div>

            @if (abaConsentimento() === 'assinatura') {
              <div class="toggles">
                <label>
                  <input type="checkbox" [(ngModel)]="consentDados" />
                  Consentimento para tratamento de dados (obrigatório para reter PII)
                </label>
                <label>
                  <input type="checkbox" [(ngModel)]="consentWhats" />
                  Consentimento para marketing WhatsApp (opcional, independente)
                </label>
              </div>
              <sgce-signature-pad (assinado)="onAssinado($event)" />
              @if (mensagemConsent()) {
                <p class="ok">{{ mensagemConsent() }}</p>
              }
            } @else {
              <p>Peça ao eleitor apontar a câmera do próprio celular. Ele será direcionado ao WhatsApp para confirmar o opt-in.</p>
              @if (deepLink(); as dl) {
                <img [src]="dl.qrCodePngDataUri" alt="QR opt-in" class="qr" />
                <p><small><a [href]="dl.url" target="_blank" rel="noopener">{{ dl.url }}</a></small></p>
              } @else {
                <p><em>Carregando QR…</em></p>
              }
            }
          </section>
        }

        <section class="danger">
          <h3>Revogar consentimento (LGPD)</h3>
          <p>Ao revogar, os dados pessoais são anonimizados imediatamente.
            Estatísticas agregadas são preservadas (D-02).</p>
          <button class="danger" (click)="revogarPrompt()" [disabled]="revogando()">
            {{ revogando() ? 'Revogando...' : 'Revogar tratamento de dados' }}
          </button>
        </section>
      } @else {
        <p>Carregando…</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 720px; }
      dl.dados { display: grid; grid-template-columns: 140px 1fr; gap: 6px 12px; margin: 16px 0; }
      dl.dados dt { font-weight: 600; color: #475569; }
      section { background: #fff; padding: 16px; border-radius: 6px; margin-bottom: 16px; }
      section.danger { border: 1px solid #fecaca; background: #fef2f2; }
      button { padding: 8px 16px; border-radius: 4px; border: 1px solid #cbd5e1; background: #f1f5f9; cursor: pointer; }
      button.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      button.danger { background: #dc2626; color: #fff; border-color: #dc2626; }
      button:disabled { opacity: 0.55; cursor: not-allowed; }
      label { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; font-size: 14px; color: #334155; }
      select { padding: 6px; border: 1px solid #cbd5e1; border-radius: 4px; }
      .abas { display: flex; gap: 8px; margin-bottom: 12px; }
      .abas button.ativo { background: #2563eb; color: #fff; border-color: #2563eb; }
      .toggles label { flex-direction: row; align-items: center; gap: 8px; }
      .intencoes { border: 1px solid #e2e8f0; padding: 10px; border-radius: 4px; margin: 10px 0; }
      .linha-intencao { display: flex; gap: 8px; align-items: center; margin: 4px 0; font-size: 14px; }
      .linha-intencao label { flex-direction: row; align-items: center; gap: 4px; margin: 0; }
      .qr { width: 220px; height: 220px; border: 1px solid #e2e8f0; padding: 8px; background: #fff; }
      .ok { background: #dcfce7; color: #166534; padding: 8px; border-radius: 4px; }
    `,
  ],
})
export class EleitorDetalheComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private eleitorSvc = inject(EleitorService);
  private equipeSvc = inject(EquipeService);
  private geo = inject(GeolocationService);

  eleitor = signal<Eleitor | null>(null);
  equipes = signal<{ id: number; nome: string }[]>([]);
  candidatosVinculados = signal<Array<{ candidatoId: number }>>([]);
  tipo: TipoAbordagem = 'DOMICILIAR';
  equipeIdSel: number | null = null;
  intencoesMap: Record<number, Intencao | undefined> = {};
  opcoesIntencao: Intencao[] = ['FAVORAVEL', 'INDECISO', 'CONTRARIO', 'HOSTIL'];

  abordagemAtualId = signal<number | null>(null);
  abaConsentimento = signal<'assinatura' | 'qr'>('assinatura');
  consentDados = true;
  consentWhats = false;
  deepLink = signal<DeepLinkOptIn | null>(null);
  mensagemConsent = signal<string | null>(null);
  revogando = signal(false);
  registrando = signal(false);

  temIntencaoValida = computed(() => Object.values(this.intencoesMap).some(v => !!v));

  async ngOnInit(): Promise<void> {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    const e = await firstValueFrom(this.eleitorSvc.buscar(id));
    this.eleitor.set(e);
    try {
      const eqs = await firstValueFrom(this.equipeSvc.listar());
      this.equipes.set(eqs.map(x => ({ id: x.id, nome: x.nome })));
    } catch {
      /* ignore */
    }
  }

  async carregarCandidatos(): Promise<void> {
    this.intencoesMap = {};
    if (!this.equipeIdSel) {
      this.candidatosVinculados.set([]);
      return;
    }
    try {
      const cs = await firstValueFrom(this.equipeSvc.listarCandidatosDaEquipe(this.equipeIdSel));
      this.candidatosVinculados.set(cs.map(c => ({ candidatoId: c.candidatoId })));
    } catch {
      this.candidatosVinculados.set([]);
    }
  }

  async registrarAbordagem(): Promise<void> {
    const e = this.eleitor();
    if (!e) return;
    this.registrando.set(true);
    try {
      const g = await this.geo.obterPosicao();
      const intencoes = Object.entries(this.intencoesMap)
        .filter(([, v]) => !!v)
        .map(([k, v]) => ({ candidatoId: Number(k), intencao: v as Intencao }));
      const abord = await firstValueFrom(
        this.eleitorSvc.registrarAbordagem({
          eleitorId: e.id,
          equipeId: this.equipeIdSel,
          tipoAbordagem: this.tipo,
          geolocalizacao: g,
          timestampLocal: new Date().toISOString(),
          intencoes,
        }),
      );
      this.abordagemAtualId.set(abord.id);
    } finally {
      this.registrando.set(false);
    }
  }

  async onAssinado(pngDataUri: string | null): Promise<void> {
    const e = this.eleitor();
    const abordId = this.abordagemAtualId();
    if (!e || !abordId || !pngDataUri) return;
    const g = await this.geo.obterPosicao();
    const c = await firstValueFrom(
      this.eleitorSvc.capturarConsentimento({
        eleitorId: e.id,
        abordagemId: abordId,
        metodoCaptura: 'ASSINATURA_TELA',
        geolocalizacao: g,
        timestampLocal: new Date().toISOString(),
        consentimentoDados: this.consentDados,
        consentimentoWhatsappMarketing: this.consentWhats,
      }),
    );
    await firstValueFrom(this.eleitorSvc.anexarAssinatura(c.id, pngDataUri));
    this.mensagemConsent.set('Consentimento #' + c.id + ' capturado e assinatura anexada.');
  }

  async solicitarDeepLink(): Promise<void> {
    const abordId = this.abordagemAtualId();
    const candidatos = this.candidatosVinculados();
    if (!abordId || candidatos.length === 0) return;
    const dl = await firstValueFrom(
      this.eleitorSvc.gerarDeepLink(abordId, candidatos[0].candidatoId),
    );
    this.deepLink.set(dl);
  }

  async revogarPrompt(): Promise<void> {
    const e = this.eleitor();
    if (!e) return;
    const confirmar = confirm(
      'Anonimizar os dados pessoais deste eleitor. Isso é irreversível. Confirmar?',
    );
    if (!confirmar) return;
    this.revogando.set(true);
    try {
      const atualizado = await firstValueFrom(this.eleitorSvc.anonimizar(e.id));
      this.eleitor.set(atualizado);
    } finally {
      this.revogando.set(false);
    }
  }
}
