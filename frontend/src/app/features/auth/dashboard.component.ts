import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'sgce-dashboard',
  standalone: true,
  template: `
    <h1>Painel</h1>
    @if (auth.user(); as u) {
      <p>Bem-vindo, <strong>{{ u.nome }}</strong> ({{ u.perfil }}).</p>
      <p>Use o menu à esquerda para navegar. As telas visíveis dependem do seu perfil.</p>
    }
  `,
})
export class DashboardComponent {
  auth = inject(AuthService);
}
