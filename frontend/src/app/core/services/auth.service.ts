import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { LoginRequest, Usuario } from '../models/usuario.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly usuarioActual = signal<Usuario | null>(null);
  readonly usuario = this.usuarioActual.asReadonly();

  constructor(private http: HttpClient) {}

  login(credenciales: LoginRequest): Observable<Usuario> {
    return this.http
      .post<ApiResponse<Usuario>>(`${environment.apiUrl}/auth/login`, credenciales)
      .pipe(
        map((respuesta) => respuesta.data),
        tap((usuario) => this.usuarioActual.set(usuario)),
      );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/auth/logout`, {})
      .pipe(tap(() => this.usuarioActual.set(null)));
  }

  estaAutenticado(): boolean {
    return this.usuarioActual() !== null;
  }

  esAdmin(): boolean {
    return this.usuarioActual()?.rol === 'ADMIN';
  }
}
