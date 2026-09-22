import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  input,
} from '@angular/core';
import SignaturePad from 'signature_pad';

/**
 * Canvas de captura de assinatura. Emite (assinado) com PNG base64 (data URI)
 * ao clicar "Confirmar", ou null ao limpar.
 */
@Component({
  selector: 'sgce-signature-pad',
  standalone: true,
  template: `
    <div class="pad-container">
      <canvas #canvas [width]="width()" [height]="height()"></canvas>
      <div class="acoes">
        <button type="button" (click)="limpar()">Limpar</button>
        <button type="button" class="primary" (click)="confirmar()" [disabled]="vazio()">
          Confirmar assinatura
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .pad-container { display: flex; flex-direction: column; gap: 8px; }
      canvas { border: 2px dashed #94a3b8; border-radius: 6px; background: #fff; touch-action: none; width: 100%; max-width: 100%; }
      .acoes { display: flex; gap: 8px; justify-content: flex-end; }
      button { padding: 8px 16px; border: 1px solid #cbd5e1; background: #f1f5f9; border-radius: 4px; cursor: pointer; }
      button.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
      button.primary:disabled { background: #94a3b8; border-color: #94a3b8; cursor: not-allowed; }
    `,
  ],
})
export class SignaturePadComponent implements AfterViewInit, OnDestroy {
  readonly width = input<number>(480);
  readonly height = input<number>(180);

  @Output() readonly assinado = new EventEmitter<string | null>();
  @ViewChild('canvas', { static: true }) private canvasRef!: ElementRef<HTMLCanvasElement>;
  private pad?: SignaturePad;

  ngAfterViewInit(): void {
    this.pad = new SignaturePad(this.canvasRef.nativeElement, {
      backgroundColor: 'rgba(255,255,255,1)',
      penColor: '#0f172a',
      minWidth: 1,
      maxWidth: 2.5,
    });
  }

  ngOnDestroy(): void {
    this.pad?.off();
  }

  vazio(): boolean {
    return !this.pad || this.pad.isEmpty();
  }

  limpar(): void {
    this.pad?.clear();
    this.assinado.emit(null);
  }

  confirmar(): void {
    if (!this.pad || this.pad.isEmpty()) return;
    this.assinado.emit(this.pad.toDataURL('image/png'));
  }
}
