import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.estaAutenticado()) {
    return true;
  }

  // No hay usuario en memoria (tipicamente: refresh completo del navegador). Antes de
  // asumir "no hay sesion" y mandar a /login, se confirma contra el backend si la cookie
  // HttpOnly sigue siendo valida (AuthService.verificarSesion) - evita un logout forzado
  // en cada F5 cuando la sesion sigue vigente en el servidor.
  return authService.verificarSesion().pipe(
    map((sesionValida) => {
      if (sesionValida) {
        return true;
      }
      router.navigate(['/login']);
      return false;
    }),
  );
};
