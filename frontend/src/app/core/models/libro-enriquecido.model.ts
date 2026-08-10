import { Libro } from './libro.model';

export interface LibroEnriquecido {
  libro: Libro;
  tituloOpenLibrary: string | null;
  coverUrl: string | null;
  numeroPaginas: number | null;
  descripcionOpenLibrary: string | null;
}
