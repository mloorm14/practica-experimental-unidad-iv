# Auditoría Lighthouse — Frontend Angular (Unidad IV)

Reporte generado con **Lighthouse** sobre el build de producción del frontend Angular.

- URL auditada: `http://localhost:8080/login` (redirección SPA desde `/`).
- Fecha: 2026-08-10.
- Entorno: build de producción servido localmente (`frontend/dist/frontend/browser`), porque el daemon de Docker no estaba levantado en la máquina de trabajo para auditar `http://localhost/` a través del stack completo.

## Puntajes obtenidos

| Categoría          | Puntaje |
|--------------------|---------|
| Performance        | 94      |
| Accessibility      | 96      |
| Best Practices     | 100     |
| SEO                | 82      |

## Archivos

- `lighthouse.report.json` — reporte completo en formato JSON.
- `lighthouse.report.html` — reporte visual navegable en el navegador.

## Notas

- La auditoría se corrió contra la app ya dockerizable (mismo `dist` que sirve el contenedor `frontend` del `docker-compose.yml`), con el service worker de PWA activo (`ngsw.json`, `ngsw-worker.js` y `manifest.webmanifest` incluidos en el build).
- Para reproducir con el stack completo: `docker compose up -d --build` y `npx lighthouse http://localhost/ --output=html --output-path=lighthouse.report.html`.
