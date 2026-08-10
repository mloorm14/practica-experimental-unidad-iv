# Auditoría Lighthouse — Frontend Angular (Unidad IV)

Reporte generado con **Lighthouse** sobre la app dockerizada corriendo en `http://localhost/` (stack completo del `docker-compose.yml`: postgres, redis, backend Spring Boot, frontend Angular servido por Nginx y nginx reverse proxy).

- URL auditada: `http://localhost/login` (redirección SPA desde `/`).
- Fecha: 2026-08-10.

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

## Reproducción

```bash
docker compose up -d --build
npx lighthouse http://localhost/ --output=html --output-path=lighthouse.report.html
```

## Notas

- El service worker de PWA (`ngsw.json`, `ngsw-worker.js` y `manifest.webmanifest`) se sirve correctamente a través del reverse proxy, verificado en la misma corrida.
- La categoría "Agentic Browsing" (0.67) es nueva en Lighthouse 12 y mide aptitudes de autónomo/navegación; no forma parte de las 4 categorías exigidas por el curso.
