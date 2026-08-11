import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { LoginRequest, Usuario } from '../models/usuario.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly usuarioActual = signal<Usuario | null>(null);
  readonly usuario = this.usuarioActual.asReadonly();

  // El JWT vive en una cookie HttpOnly (correcto: ilegible desde JS, mitiga robo via XSS),
  // asi que tras un refresh completo del navegador usuarioActual vuelve a null aunque la
  // cookie siga siendo valida en el servidor - no hay forma de leerla desde el frontend.
  // El backend no expone un endpoint tipo /auth/me para reconstruir el usuario a partir de
  // la cookie (deliberadamente no se agrega uno en este cambio de frontend). Como solucion
  // minima, sesionVerificada confirma la validez de la cookie de forma indirecta golpeando
  // un endpoint protegido liviano ya existente (GET /api/v1/libros) al arrancar la app: si
  // responde 200, la sesion es valida y se deja de redirigir a /login en cada refresh, pero
  // username/rol quedan sin reconstruir hasta el proximo login() real - esAdmin() devuelve
  // false en ese estado intermedio (oculta acciones de ADMIN hasta la siguiente interaccion),
  // tradeoff aceptado explicitamente en vez de inventar un endpoint nuevo en este PR.
  private readonly sesionVerificada = signal(false);

  constructor(private http: HttpClient) {}

  login(credenciales: LoginRequest): Observable<Usuario> {
    return this.http
      .post<ApiResponse<Usuario>>(`${environment.apiUrl}/auth/login`, credenciales)
      .pipe(
        map((respuesta) => respuesta.data),
        tap((usuario) => {
          this.usuarioActual.set(usuario);
          this.sesionVerificada.set(true);
        }),
      );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/auth/logout`, {})
      .pipe(
        tap(() => {
          this.usuarioActual.set(null);
          this.sesionVerificada.set(false);
        }),
      );
  }

  estaAutenticado(): boolean {
    return this.usuarioActual() !== null || this.sesionVerificada();
  }

  esAdmin(): boolean {
    return this.usuarioActual()?.rol === 'ADMIN';
  }

  verificarSesion(): Observable<boolean> {
    const params = new HttpParams().set('page', 0).set('size', 1);
    return this.http.get<ApiResponse<unknown>>(`${environment.apiUrl}/libros`, { params }).pipe(
      map(() => {
        this.sesionVerificada.set(true);
        return true;
      }),
      catchError(() => {
        this.sesionVerificada.set(false);
        return of(false);
      }),
    );
  }
}
