// Coincide exactamente con LibroResponse.java (backend): el catalogo (editorial/idioma/
// estado) viaja como par Id (para formularios) + Nombre (para mostrar), nunca como un
// campo de texto plano "editorial"/"idioma"/"estado".
export interface Libro {
  id: number;
  titulo: string;
  descripcion: string;
  isbn: string;
  genero: string;
  autor: string;
  anioPublicacion: number;
  editorialId: number;
  editorialNombre: string;
  idiomaId: number;
  idiomaNombre: string;
  estadoId: number;
  estadoNombre: string;
  stock: number;
  createdAt: string;
  updatedAt: string;
}

export type { PageResponse } from './page-response.model';
