import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LibroService } from '../../core/services/libro.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { Libro } from '../../core/models/libro.model';

@Component({
  selector: 'app-libros',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './libros.html',
  styleUrl: './libros.scss',
})
export class Libros implements OnInit {
  libros = signal<Libro[]>([]);
  cargando = signal(false);
  paginaActual = signal(0);
  totalPaginas = signal(0);

  constructor(
    private libroService: LibroService,
    private authService: AuthService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    this.cargarLibros();
  }

  esAdmin(): boolean {
    return this.authService.esAdmin();
  }

  cargarLibros(): void {
    this.cargando.set(true);
    this.libroService.listar(this.paginaActual()).subscribe({
      next: (respuesta) => {
        this.libros.set(respuesta.content);
        this.totalPaginas.set(respuesta.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.notificationService.mostrarError('No se pudieron cargar los libros.');
      },
    });
  }

  eliminar(id: number): void {
    if (!confirm('¿Seguro que deseas eliminar este libro?')) {
      return;
    }

    this.libroService.eliminar(id).subscribe({
      next: () => {
        this.notificationService.mostrarExito('Libro eliminado correctamente.');
        this.cargarLibros();
      },
      error: () => this.notificationService.mostrarError('No se pudo eliminar el libro.'),
    });
  }

  siguientePagina(): void {
    if (this.paginaActual() + 1 < this.totalPaginas()) {
      this.paginaActual.set(this.paginaActual() + 1);
      this.cargarLibros();
    }
  }

  paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.paginaActual.set(this.paginaActual() - 1);
      this.cargarLibros();
    }
  }
}
