import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { Autor } from '../models/autor.model';
import { PageResponse } from '../models/page-response.model';
import { ApiResponse } from '../models/api-response.model';

export type AutorRequest = Omit<Autor, 'id' | 'createdAt' | 'updatedAt'>;

@Injectable({ providedIn: 'root' })
export class AutorService {
  constructor(private http: HttpClient) {}

  listar(pagina: number = 0, tamanio: number = 10): Observable<PageResponse<Autor>> {
    const params = new HttpParams().set('page', pagina).set('size', tamanio);

    return this.http.get<ApiResponse<Autor[]>>(`${environment.apiUrl}/autores`, { params }).pipe(
      map((respuesta) => ({
        content: respuesta.data,
        totalElements: Number(respuesta.meta['total'] ?? 0),
        totalPages: Number(respuesta.meta['last_page'] ?? 0),
        number: Number(respuesta.meta['current_page'] ?? 0),
        size: tamanio,
      })),
    );
  }

  obtenerPorId(id: number): Observable<Autor> {
    return this.http
      .get<ApiResponse<Autor>>(`${environment.apiUrl}/autores/${id}`)
      .pipe(map((respuesta) => respuesta.data));
  }

  crear(autor: AutorRequest): Observable<Autor> {
    return this.http
      .post<ApiResponse<Autor>>(`${environment.apiUrl}/autores`, autor)
      .pipe(map((respuesta) => respuesta.data));
  }

  actualizar(id: number, autor: AutorRequest): Observable<Autor> {
    return this.http
      .put<ApiResponse<Autor>>(`${environment.apiUrl}/autores/${id}`, autor)
      .pipe(map((respuesta) => respuesta.data));
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/autores/${id}`);
  }
}
