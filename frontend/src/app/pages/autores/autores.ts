import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AutorService } from '../../core/services/autor.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { Autor } from '../../core/models/autor.model';

@Component({
  selector: 'app-autores',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './autores.html',
  styleUrl: './autores.scss',
})
export class Autores implements OnInit {
  autores = signal<Autor[]>([]);
  cargando = signal(false);
  paginaActual = signal(0);
  totalPaginas = signal(0);

  constructor(
    private autorService: AutorService,
    private authService: AuthService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    this.cargarAutores();
  }

  esAdmin(): boolean {
    return this.authService.esAdmin();
  }

  cargarAutores(): void {
    this.cargando.set(true);
    this.autorService.listar(this.paginaActual()).subscribe({
      next: (respuesta) => {
        this.autores.set(respuesta.content);
        this.totalPaginas.set(respuesta.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.notificationService.mostrarError('No se pudieron cargar los autores.');
      },
    });
  }

  eliminar(id: number): void {
    if (!confirm('¿Seguro que deseas eliminar este autor?')) {
      return;
    }

    this.autorService.eliminar(id).subscribe({
      next: () => {
        this.notificationService.mostrarExito('Autor eliminado correctamente.');
        this.cargarAutores();
      },
      error: () => this.notificationService.mostrarError('No se pudo eliminar el autor.'),
    });
  }

  siguientePagina(): void {
    if (this.paginaActual() + 1 < this.totalPaginas()) {
      this.paginaActual.set(this.paginaActual() + 1);
      this.cargarAutores();
    }
  }

  paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.paginaActual.set(this.paginaActual() - 1);
      this.cargarAutores();
    }
  }
}
