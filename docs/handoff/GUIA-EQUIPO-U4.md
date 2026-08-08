# Guía de continuidad del equipo — Unidad IV

Este documento está dirigido a cualquier persona (o IA asistente) que se incorpore a este repositorio sin haber estado en el trabajo previo. Es autocontenido y es la fuente de verdad del estado actual del proyecto — si algo en otro documento contradice lo que dice aquí, este documento manda.

## 1. Contexto

**Equipo D**:
- **Marlon Loor Medranda** — tech lead, backend, infraestructura.
- **Escudero Plaza María del Rosario** — tests de integración (Unidad IV).
- **Castro Espinoza Kevin Moisés** — frontend (Unidad IV).

**Docente**: Guerrero Ulloa Gleiston Cicerón.

**Aclaración explícita**: este repositorio (`practica-experimental-unidad-iv`) es un ejercicio académico independiente de cualquier otro repositorio o proyecto del tech lead — incluyendo su Proyecto Fin de Curso real, mencionado en la "Nota de contexto" de cada ADR únicamente para dejar constancia de que el dominio de gestión bibliotecaria de este ejercicio no debe confundirse con ese otro proyecto. Nada de lo que exista en otros repositorios del tech lead aplica a este trabajo: ni convenciones, ni decisiones arquitectónicas, ni deuda técnica. Este documento y el código de este repo son la única fuente de verdad.

## 2. Arranque

```bash
git clone https://github.com/mloorm14/practica-experimental-unidad-iv.git
cd practica-experimental-unidad-iv

cp .env.example .env
# Editar .env:
#   JWT_SECRET=$(openssl rand -base64 48)
#   COOKIE_SECURE=false   (solo para desarrollo local sobre HTTP; en produccion sobre HTTPS, quitar esta linea)

docker compose up -d --build
```

Esto levanta **5 servicios**: `postgres`, `redis`, `app` (backend Spring Boot), `frontend` (Angular servido por Nginx), `nginx` (reverse proxy de entrada — único puerto publicado al host). Verificar con `docker compose ps` que los 5 estén `Up` (postgres/redis/app deben reportar `healthy`).

**Accesos**:
| Recurso | URL |
|---|---|
| Frontend | http://localhost/ |
| API REST | http://localhost/api/v1/... |
| Swagger UI | http://localhost/api/documentation (alias `/api/docs` → 302) |
| SOAP WSDL | http://localhost/ws/libro-catalogo.wsdl |

**Usuarios de prueba** (seed de Flyway, ver `V2__crear_tabla_usuarios.sql` y `V7__usuario_prueba_rol_user.sql`):
| Usuario | Password | Rol |
|---|---|---|
| `admin` | `Admin123!` | ADMIN |
| `usuario_prueba` | `User123!` | USER |

## 3. Qué está HECHO

- **API versionada `/api/v1`** + envelope `ApiResponse<T>` en todas las respuestas exitosas + `ProblemDetail` (RFC 7807) en todos los errores (`GlobalExceptionHandler.java`).
- **Swagger/OpenAPI**: springdoc-openapi 2.9.0, `OpenApiConfig.java` (bearer security scheme), `@Tag`/`@Operation` en los 6 controladores REST.
- **Roles y autenticación**: `@PreAuthorize("hasRole('ADMIN')")` + `@EnableMethodSecurity` en las 4 escrituras (crear/actualizar/eliminar) de cada recurso; JWT firmado HS384 (`JwtService.java`), extracción dual cookie `HttpOnly`/`Authorization: Bearer` (`JwtAuthenticationFilter.java`); blacklist de `jti` en Redis para logout (`TokenBlacklistService.java`); cookie con `secure` configurable vía `app.cookie.secure`/`COOKIE_SECURE` (segura por defecto).
- **5 recursos CRUD**: `Libro`, `Autor`, `Editorial`, `Idioma`, `EstadoLibro`. Incluye relación N:M real `Libro`↔`Autor` (tabla `libro_autor`, migración V8) — **el campo legado `libros.autor` (String) sigue existiendo, marcado `@Deprecated`, pendiente de eliminar en una migración V9 futura** (ver punto 4, "CONJUNTO").
- **Open Library**: `OpenLibraryClient.java`, cache-aside 24h en un namespace de cache separado (`openlibrary_isbn`), endpoint `GET /api/v1/libros/{id}/enriquecido`.
- **SOAP**: Spring-WS contract-first (`libro-catalogo.xsd` → JAXB), `LibroCatalogoEndpoint.java`, WSDL en `/ws/libro-catalogo.wsdl`.
- **Docker Compose de 5 servicios + Nginx reverse proxy**: `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `nginx/nginx.conf`.
- **Apache Bench + auditoría OWASP + colección Postman**: `docs/informe/apache-bench.md`, `docs/informe/auditoria-owasp.md`, `docs/postman/SGB-API.postman_collection.json` (29 requests).
- **ADR-001 a ADR-005** (`docs/adr/`): arquitectura en capas, Angular vs. React, Redis (cache + blacklist JWT), Hibernate/JPA vs. alternativas, selección de Spring Boot.
- **Diagramas C4** niveles 1-3 (`docs/arquitectura/`).
- **36 tests** (unit + repository con Testcontainers), `./mvnw -B clean verify` en verde.

## 4. Qué FALTA — asignación nominal

### MARÍA — Tests de integración

El PDF (OE1) exige un mínimo de **10 feature/integration tests**. Los 36 tests actuales son unitarios (`service/`, con Mockito) y de repositorio (`repository/`, con Testcontainers) — **no hay tests de integración HTTP end-to-end todavía**.

**Dónde**: crear el paquete `backend/src/test/java/ec/edu/uteq/pfcbackend/integration/`.

**Cómo**: `@SpringBootTest` + `@AutoConfigureMockMvc`, reutilizando el bootstrap de Testcontainers de [`LibroRepositoryTest.java`](../../backend/src/test/java/ec/edu/uteq/pfcbackend/repository/LibroRepositoryTest.java) (líneas 1-39: el `@Container static PostgreSQLContainer<?>` + el método `@DynamicPropertySource configurarPropiedades`) — es el mismo patrón, solo cambia la anotación de clase de `@DataJpaTest` a `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + `@AutoConfigureMockMvc`.

**Mínimo 10 tests, casos concretos**:
1. Login con credenciales correctas → 200, cookie `Set-Cookie` presente.
2. Login con credenciales inválidas → 401.
3. `GET /api/v1/libros` sin token → 401.
4. `GET /api/v1/libros` con rol USER → 200.
5. `POST /api/v1/libros` con rol USER → 403.
6. `POST /api/v1/libros` con rol ADMIN, body válido → 201.
7. `GET /api/v1/libros/{id}` con id inexistente → 404 con `ProblemDetail`.
8. `POST /api/v1/libros` con body inválido (campos `@NotBlank`/`@NotNull` faltantes) → 400 con el array `errors` poblado.
9. Logout invalida el token: login → logout → reutilizar el mismo token → 401 (mismo patrón ya verificado manualmente con curl en `docs/informe/auditoria-owasp.md`, sección A07 — aquí se automatiza).
10. Cualquier respuesta exitosa (ej. el login o el listado) trae el envelope `ApiResponse` (`success`, `data`, `message`, `errors`, `meta`), no la entidad "pelada".

### KEVIN — Frontend

1. **CRUD de Autor** consumiendo `/api/v1/autores`. Copiar literalmente el patrón de [`libro.service.ts`](../../frontend/src/app/core/services/libro.service.ts): cada método hace `this.http.<verbo>(...)` tipado como `Observable<ApiResponse<T>>` y usa `.pipe(map((respuesta) => respuesta.data))` para desenvolver el `.data` del envelope antes de devolverlo al componente — el componente nunca debe ver el `ApiResponse` crudo.
2. **Mostrar datos de Open Library en la UI** (exigido explícitamente por OE3), consumiendo `GET /api/v1/libros/{id}/enriquecido`. La respuesta trae `{ libro: {...}, tituloOpenLibrary, coverUrl, numeroPaginas, descripcionOpenLibrary }` (ver [`LibroEnriquecidoResponse.java`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/dto/LibroEnriquecidoResponse.java)) — los 4 campos de Open Library **pueden venir `null`** (ISBN no encontrado o servicio caído), la UI debe manejarlo sin romperse (mostrar un placeholder o simplemente omitir el campo, no asumir que siempre hay valor).
3. **Vistas condicionadas por rol** (ocultar crear/editar/eliminar si el usuario no es ADMIN). El rol **sí viaja ya en el body de la respuesta de login** — confirmado en el código real: `LoginResponse` es `record LoginResponse(String username, String rol)` y `AuthController.login()` devuelve `ApiResponse.success(new LoginResponse(usuario.getUsername(), usuario.getRol()))`. No hace falta decodificar el JWT en el frontend (que además sería imposible: la cookie es `HttpOnly`, ilegible desde JS por diseño) — guardar el `rol` recibido en el login (ej. en un servicio de sesión/estado del frontend) y usarlo para condicionar la UI.
4. **PWA**: `ng add @angular/pwa` (exigido solo por el SGA, el PDF no lo menciona — ver `DECISIONES-GUIA-VS-ENUNCIADO-U4.md`, punto 7).
5. **Lighthouse**: correr auditoría y guardar el reporte en `docs/informe/`.

### TODOS — Informe técnico final

**Contexto de por qué esto es crítico**: en la Unidad III el informe técnico se entregó incompleto y costó **0/10, 0/5, 0/13** en los criterios correspondientes. No repetir ese error — el código puede estar perfecto y el informe sigue siendo una entrega separada y obligatoria.

Estructura (usar `docs/informe/Informe-Tecnico-Unidad-III-Equipo-D.docx` como plantilla de formato):
- Portada.
- Resumen bilingüe (español + inglés).
- Introducción.
- 4 secciones de fundamento teórico, **mínimo 350 palabras cada una**:
  - 5.1 — MVC + comparativa de frameworks, citando [ADR-005](../adr/ADR-005-seleccion-framework-spring-boot.md).
  - 5.2 — REST + Fielding (2000) + JWT + OpenAPI.
  - 5.3 — SOAP vs. REST, tabla con **mínimo 8 criterios**, usando `docs/informe/ejemplo-soap-vs-rest.md` como base real (no reescribir desde cero, expandir esa base).
  - 5.4 — Seguridad + rendimiento + Docker, usando `docs/informe/auditoria-owasp.md` y `docs/informe/apache-bench.md` como evidencia real (no repetir contenido teórico genérico que ya está cubierto por esos documentos, referenciarlos).
- Sección de tendencias, **mínimo 400 palabras**: Jamstack + PWA + IA generativa, con posición crítica del equipo (no solo descriptiva).
- Reflexión individual, **mínimo 100 palabras por persona** — la de Marlon ya existe como borrador en `docs/informe/reflexion-marlon.md`; María y Kevin deben escribir la propia siguiendo el mismo criterio (hechos reales de este repositorio, no genérico).
- Conclusiones.
- Trabajo futuro.
- Referencias en **norma APA, mínimo 5 fuentes**, incluyendo obligatoriamente **Fielding (2000)** (la disertación doctoral de REST) y la **documentación oficial de Spring Boot**.
- **Anexo C**: las 4 preguntas de análisis que exige el PDF.

### CONJUNTO — deuda técnica a documentar, no a ocultar

- **Migración V9** (eliminar `libros.autor` String): hacerlo **solo después** de que el frontend consuma la relación N:M real (`Autor`) en vez del campo legado — si se elimina antes, se rompe cualquier consumidor que todavía dependa de ese campo.
- **Sin rate limiting** en `/api/v1/auth/login` ni en ningún otro endpoint — documentado como gap real en `docs/informe/auditoria-owasp.md` (A04/A07), incluirlo también en la sección "trabajo futuro" del informe.
- **`jjwt` desactualizado**: 0.12.6, con 0.13.0 disponible — documentado en la misma auditoría (A06), incluir en trabajo futuro.

## 5. Convenciones

- Commits en español, imperativo, formato Conventional Commits (`feat:`, `fix:`, `docs:`, etc.).
- **Sin trailers de IA** en ningún commit.
- Autor del commit = persona real del equipo, con correo institucional (no una cuenta genérica ni un asistente).
- Ramas: `feature/nombre-corto`.
- `./mvnw -B clean verify` en verde **antes** de cada commit que toque el backend — no después.

## 6. Puntos de atención técnica

- `spring.jpa.hibernate.ddl-auto: validate` — el esquema **solo** se gestiona por Flyway (`backend/src/main/resources/db/migration/`). Nunca cambiar a `update`/`create`, ni siquiera "temporalmente" para probar algo local.
- Si se agrega un nuevo listado paginado con cache, reutilizar `CacheablePage` (`backend/src/main/java/ec/edu/uteq/pfcbackend/config/CacheablePage.java`, ya usado en `LibroServiceImpl`) en vez de cachear un `Page<T>` de Spring Data directamente (no es serializable de forma estable para Redis).
- En tests que usan el seed de Flyway (usuarios, datos semilla), usar `deleteAllInBatch()` en la limpieza entre tests, **no** `deleteAll()` — `deleteAll()` dispara un `SELECT` + `DELETE` por fila y puede chocar con las filas sembradas por las migraciones de forma más frágil que un `deleteAllInBatch()` directo.
- `ApiResponse` únicamente en respuestas exitosas; `ProblemDetail` únicamente en errores. No mezclar los dos formatos en el mismo endpoint.
- Cualquier endpoint nuevo debe llevar: versionado bajo `/v1`, envuelto en `ApiResponse`, `@PreAuthorize` si es una escritura, y `@Operation` de Swagger — los 4, no un subconjunto.
- `container_name` se quitó **a propósito** de todos los servicios de `docker-compose.yml` (evita colisiones con contenedores de otros proyectos no relacionados en la misma máquina — ver el commit `50f1253`). No volver a agregarlo.
