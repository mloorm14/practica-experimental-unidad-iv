import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { Libro, PageResponse } from '../models/libro.model';
import { ApiResponse } from '../models/api-response.model';

export type LibroRequest = Omit<Libro, 'id' | 'createdAt' | 'updatedAt'>;

@Injectable({ providedIn: 'root' })
export class LibroService {
  constructor(private http: HttpClient) {}

  listar(pagina: number = 0, tamanio: number = 10): Observable<PageResponse<Libro>> {
    const params = new HttpParams()
      .set('page', pagina)
      .set('size', tamanio);

    return this.http
      .get<ApiResponse<Libro[]>>(`${environment.apiUrl}/libros`, { params })
      .pipe(
        map((respuesta) => ({
          content: respuesta.data,
          totalElements: Number(respuesta.meta['total'] ?? 0),
          totalPages: Number(respuesta.meta['last_page'] ?? 0),
          number: Number(respuesta.meta['current_page'] ?? 0),
          size: tamanio,
        })),
      );
  }

  obtenerPorId(id: number): Observable<Libro> {
    return this.http
      .get<ApiResponse<Libro>>(`${environment.apiUrl}/libros/${id}`)
      .pipe(map((respuesta) => respuesta.data));
  }

  crear(libro: LibroRequest): Observable<Libro> {
    return this.http
      .post<ApiResponse<Libro>>(`${environment.apiUrl}/libros`, libro)
      .pipe(map((respuesta) => respuesta.data));
  }

  actualizar(id: number, libro: LibroRequest): Observable<Libro> {
    return this.http
      .put<ApiResponse<Libro>>(`${environment.apiUrl}/libros/${id}`, libro)
      .pipe(map((respuesta) => respuesta.data));
  }
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/libros/${id}`);
  }
}
