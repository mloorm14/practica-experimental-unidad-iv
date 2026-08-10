export interface Libro {
  id: number;
  titulo: string;
  descripcion: string;
  isbn: string;
  genero: string;
  autor: string;
  anioPublicacion: number;
  editorial: string;
  idioma: string;
  estado: string;
  stock: number;
  createdAt: string;
  updatedAt: string;
}

export type { PageResponse } from './page-response.model';
