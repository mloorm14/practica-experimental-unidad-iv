import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { LibroService, LibroRequest } from '../../core/services/libro.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-libro-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './libro-form.html',
  styleUrl: './libro-form.scss'
})
export class LibroForm implements OnInit {
  esEdicion = signal(false);
  guardando = signal(false);
  private libroId: number | null = null;

  form: LibroRequest = {
    titulo: '',
    descripcion: '',
    isbn: '',
    genero: '',
    autor: '',
    anioPublicacion: new Date().getFullYear(),
    editorial: '',
    idioma: '',
    estado: '',
    stock: 0
  };

  constructor(
    private libroService: LibroService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.libroId = Number(idParam);
      this.esEdicion.set(true);
      this.libroService.obtenerPorId(this.libroId).subscribe({
        next: (libro) => {
          // Libro (lectura) y LibroRequest (escritura) ya no comparten shape: Libro trae
          // editorialId/editorialNombre (etc.), pero el formulario todavia pide editorial/
          // idioma/estado como texto libre (limitacion preexistente del formulario, fuera
          // del alcance de este fix) - se preservan en blanco, igual que el comportamiento
          // previo a este cambio.
          this.form = {
            titulo: libro.titulo,
            descripcion: libro.descripcion,
            isbn: libro.isbn,
            genero: libro.genero,
            autor: libro.autor,
            anioPublicacion: libro.anioPublicacion,
            editorial: '',
            idioma: '',
            estado: '',
            stock: libro.stock
          };
        }
      });
    }
  }

  onSubmit(): void {
    this.guardando.set(true);

    const peticion = this.esEdicion()
      ? this.libroService.actualizar(this.libroId!, this.form)
      : this.libroService.crear(this.form);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.mostrarExito('Libro guardado correctamente.');
        this.router.navigate(['/libros']);
      },
      error: () => {
        this.guardando.set(false);
        this.notificationService.mostrarError('No se pudo guardar el libro. Verifica los datos.');
      }
    });
  }
}
