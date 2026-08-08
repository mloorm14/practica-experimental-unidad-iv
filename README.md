# Práctica Experimental - Unidad IV (PFC)

Proyecto de Fin de Curso (PFC) grupal desarrollado como parte de la Unidad IV de la asignatura — Sistema de Gestión Bibliotecaria (SGB), sobre la base construida en la Unidad III.

## Stack tecnológico

- **Backend:** Java 21 + Spring Boot 3.5.x
- **Frontend:** Angular 22
- **Base de datos:** PostgreSQL 16
- **Cache:** Redis 7
- **Documentación API:** springdoc-openapi (Swagger UI)
- **Servicio SOAP:** Spring-WS (contract-first, WSDL en `/ws/libro-catalogo.wsdl`)
- **Reverse proxy / infraestructura:** Nginx + Docker Compose (5 servicios: postgres, redis, app, frontend, nginx)

## Integrantes del equipo

Equipo D

- **Loor Medranda Marlon Taylor** — Tech lead, backend, infraestructura (API REST versionada, seguridad JWT/roles, cache-aside con Redis, integración Open Library, servicio SOAP, Docker Compose + Nginx, prueba de carga, auditoría OWASP, colección Postman, ADRs).
- **Escudero Plaza María del Rosario** — Tests de integración (feature tests HTTP end-to-end sobre la API).
- **Castro Espinoza Kevin Moisés** — Frontend Angular (CRUD de Autor, integración con Open Library en la UI, vistas condicionadas por rol, PWA).

## Estado del proyecto

Para el estado actual del proyecto (qué está hecho, qué falta y quién lo haría, cómo levantar el entorno completo, y puntos de atención técnica) — **fuente de verdad**: [docs/handoff/GUIA-EQUIPO-U4.md](docs/handoff/GUIA-EQUIPO-U4.md).

Para las discrepancias entre el PDF de la guía y el enunciado/SGA ya resueltas en el código: [docs/handoff/DECISIONES-GUIA-VS-ENUNCIADO-U4.md](docs/handoff/DECISIONES-GUIA-VS-ENUNCIADO-U4.md).

La documentación equivalente de la Unidad III se conserva como referencia histórica en la misma carpeta (`docs/handoff/*-U3.md`).

## Estructura del proyecto

```
.
├── backend/              # API Spring Boot (Java 21)
├── frontend/             # Aplicación Angular (Nginx en runtime del contenedor)
├── nginx/                # Reverse proxy de entrada (docker-compose)
│   └── nginx.conf
├── docs/
│   ├── adr/              # Architecture Decision Records (ADR-001 a ADR-005)
│   ├── arquitectura/     # Diagramas C4 (niveles 1-3) y documentación de arquitectura
│   ├── handoff/          # Guías de continuidad para el equipo (U3 historico + U4 vigente)
│   ├── informe/          # Informes técnicos, auditoría OWASP, benchmark Apache Bench
│   └── postman/          # Colección y environment de Postman
├── docker-compose.yml    # postgres + redis + app + frontend + nginx
├── .env.example          # Plantilla de variables de entorno
└── .env                  # Variables de entorno locales (no versionado)
```

## Puesta en marcha del entorno local

1. Copiar `.env.example` a `.env` y completar los valores:
   - `JWT_SECRET`: generar uno propio, ej. `openssl rand -base64 48`.
   - `COOKIE_SECURE=false`: solo para desarrollo local sobre HTTP plano (sin TLS). El valor por defecto en `application.yml` es `true` (seguro).

2. Levantar el sistema completo:

   ```bash
   docker compose up -d --build
   ```

   Esto construye y levanta **5 servicios**: `postgres`, `redis`, `app` (backend), `frontend` (Angular servido por Nginx), `nginx` (reverse proxy de entrada, único puerto publicado al host).

3. Verificar que los contenedores estén saludables:

   ```bash
   docker compose ps
   ```

4. Para detener los servicios:

   ```bash
   docker compose down
   ```

## URLs útiles (con el stack levantado)

| Recurso | URL |
|---|---|
| Frontend | http://localhost/ |
| API REST | http://localhost/api/v1/... |
| Swagger UI | http://localhost/api/documentation (alias `/api/docs`) |
| SOAP WSDL | http://localhost/ws/libro-catalogo.wsdl |

Usuarios de prueba: `admin` / `Admin123!` (rol ADMIN), `usuario_prueba` / `User123!` (rol USER).
