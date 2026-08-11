import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { LibroService } from '../../core/services/libro.service';
import { LibroEnriquecido } from '../../core/models/libro-enriquecido.model';

@Component({
  selector: 'app-libro-detalle',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './libro-detalle.html',
  styleUrl: './libro-detalle.scss',
})
export class LibroDetalle implements OnInit {
  enriquecido = signal<LibroEnriquecido | null>(null);
  cargando = signal(false);
  error = signal(false);

  constructor(
    private libroService: LibroService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.cargar(Number(idParam));
    }
  }

  cargar(id: number): void {
    this.cargando.set(true);
    this.error.set(false);
    this.libroService.obtenerEnriquecido(id).subscribe({
      next: (respuesta) => {
        this.enriquecido.set(respuesta);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }
}
