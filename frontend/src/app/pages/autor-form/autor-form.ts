import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AutorService, AutorRequest } from '../../core/services/autor.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-autor-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './autor-form.html',
  styleUrl: './autor-form.scss',
})
export class AutorForm implements OnInit {
  esEdicion = signal(false);
  guardando = signal(false);
  private autorId: number | null = null;

  form: AutorRequest = {
    nombre: '',
    nacionalidad: '',
  };

  constructor(
    private autorService: AutorService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.autorId = Number(idParam);
      this.esEdicion.set(true);
      this.autorService.obtenerPorId(this.autorId).subscribe({
        next: (autor) => {
          const { id, createdAt, updatedAt, ...resto } = autor;
          this.form = resto;
        },
      });
    }
  }

  onSubmit(): void {
    this.guardando.set(true);

    const peticion = this.esEdicion()
      ? this.autorService.actualizar(this.autorId!, this.form)
      : this.autorService.crear(this.form);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.mostrarExito('Autor guardado correctamente.');
        this.router.navigate(['/autores']);
      },
      error: () => {
        this.guardando.set(false);
        this.notificationService.mostrarError('No se pudo guardar el autor. Verifica los datos.');
      },
    });
  }
}
