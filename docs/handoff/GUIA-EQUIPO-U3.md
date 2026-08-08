> **DOCUMENTO HISTORICO (Unidad III).** Se conserva como referencia del trabajo ya entregado y calificado. Para el estado actual del proyecto ver los documentos equivalentes de la Unidad IV en esta misma carpeta.

# Guía de continuidad del equipo

Este documento está dirigido a cualquier persona (o IA asistente, como Claude Code) que se incorpore a este repositorio sin haber estado en las conversaciones previas del equipo. Es autocontenido: no necesitas contexto externo para entender el estado del proyecto y qué falta por hacer.

## 1. Resumen del proyecto

Este es un PFC (Proyecto de Fin de Curso) universitario: un sistema de **gestión bibliotecaria** (catálogo de libros) con autenticación.

Stack: **Java 21 + Spring Boot 3.5.x** (backend), **Angular 17+** (frontend), **PostgreSQL 16** (base de datos relacional), **Redis 7** (cache + blacklist de JWT).

El proyecto se basa en dos documentos fuente: una guía de práctica general del curso y un enunciado de tarea puntual, que tienen algunas diferencias entre sí (por ejemplo, la guía sugiere MySQL y el enunciado exige PostgreSQL). Todas esas diferencias ya fueron detectadas y resueltas — el detalle completo, incoherencia por incoherencia, está en [`docs/handoff/DECISIONES-GUIA-VS-ENUNCIADO.md`](./DECISIONES-GUIA-VS-ENUNCIADO.md). No lo repetimos aquí; consúltalo si algo en el código no parece coincidir con la guía general del curso (ahí también está documentado el cambio de dominio de Producto a Libro).

> **Nota — cambio de dominio:** la versión inicial de este ejercicio modelaba un dominio genérico de "Producto/inventario". Se ajustó a "Libro/gestión bibliotecaria" para alinear el ejercicio con el contexto real de la materia (Unidad III, Aplicaciones Web). El cambio se hizo mediante una migración Flyway versionada (`V3__renombrar_producto_a_libro.sql`), sin recrear la tabla ni perder historial de commits. **Si clonaste el repo antes de este cambio**, haz `git pull` y revisa que tu trabajo en curso (ramas propias, código local sin commitear) no choque con el rename — en particular, cualquier referencia a `Producto`, `ProductoRepository`, `/api/productos`, etc. en tu copia local quedó obsoleta.

## 2. Cómo levantar el proyecto localmente desde cero

```bash
git clone https://github.com/mloorm14/practica-experimental-unidad-iii.git
cd practica-experimental-unidad-iii
cp .env.example .env
```

Abre `.env` y define tu propio `JWT_SECRET` (no reutilices el de otro entorno):

```bash
openssl rand -base64 48
```

Pega el resultado como valor de `JWT_SECRET` en `.env`. Los demás valores por defecto de `.env.example` sirven tal cual para desarrollo local.

Levanta la infraestructura (Postgres + Redis):

```bash
docker compose up -d
docker compose ps   # confirma que ambos servicios queden "healthy"
```

Arranca el backend (usa el Maven Wrapper, no necesitas Maven instalado globalmente):

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

El backend queda escuchando en `http://localhost:8080`. Flyway aplica las migraciones automáticamente al arrancar (no hay que correr nada manual).

**Usuario de prueba** (creado por la migración `V2__crear_tabla_usuarios.sql`):

- `username`: `admin`
- `password`: `Admin123!`

Prueba rápida de login:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}'
```

La respuesta trae un `Set-Cookie: access_token=...` (HttpOnly). Guarda esa cookie para llamar a cualquier endpoint bajo `/api/**` que no sea `/api/auth/**`.

Cuando termines de trabajar, baja los contenedores para no dejarlos corriendo de fondo:

```bash
docker compose down
```

## 3. Qué está HECHO y verificado

Todo lo siguiente está implementado, compilado y probado end-to-end con evidencia real (no solo "debería funcionar"):

- **Setup del repo**: estructura de carpetas, `docker-compose.yml` (Postgres 16 + Redis 7 con healthchecks), `.env`/`.env.example`, `.gitignore`.
- **CRUD completo de `Libro`** en las 4 capas (`Controller` → `Service` → `Repository` → `Entity`), con DTOs separados de la entidad, paginación vía `Pageable`, y manejo de errores centralizado (`GlobalExceptionHandler`: 404 no encontrado, 400 validación). Campos actuales de `Libro`: `titulo`, `descripcion`, `isbn`, `genero`, `autor`, `anioPublicacion`, `editorial`, `idioma`, `estado`, `stock` — nota que `editorial`, `idioma` y `estado` hoy son texto libre, no relaciones (ver sección 4, es justo lo que le toca extender al Compañero 1).
- **Cache-aside con Redis** sobre `GET /api/libros` (`@Cacheable`/`@CacheEvict`, TTL 5 minutos, cache `libros_listado`). Verificado con `redis-cli KEYS *`: la key aparece tras el primer GET y desaparece tras cualquier escritura (evict).
- **Autenticación JWT en cookie HttpOnly con blacklist de JTI en Redis**. Login/logout funcional y verificado: login devuelve cookie, logout agrega el `jti` a la blacklist con TTL igual al tiempo restante del token, y reutilizar la cookie después del logout devuelve **401** (la prueba central de que el logout invalida el token de verdad).
- **Diagramas C4 nivel 1 (contexto) y nivel 2 (contenedores)** en Structurizr DSL, exportados a PNG (`docs/arquitectura/`).
- **3 ADR** (Architecture Decision Records) en `docs/adr/`: arquitectura en capas, Angular vs. React, Redis para cache + blacklist JWT.

## 4. Qué falta y quién lo haría

### Compañero 1: entidades maestras (Editorial, Idioma, EstadoLibro) + datos semilla

**Qué es exactamente "el resto de entidades"** (esto generó confusión real en el equipo, así que va explícito): son las **3 tablas maestras/catálogo** que en el dominio real de gestión bibliotecaria se relacionan con `Libro` por foreign key: **`Editorial`**, **`Idioma`** y **`EstadoLibro`**. Hoy `Libro` tiene `editorial`, `idioma` y `estado` como columnas de texto libre (`VARCHAR`) — es una simplificación deliberada de la Unidad III, documentada en `V3__renombrar_producto_a_libro.sql`, a la espera de que este trabajo las convierta en relaciones reales.

Pasos concretos:

1. **Migraciones Flyway** para las 3 tablas maestras — puede ser una sola `V4__crear_tablas_maestras.sql` con las 3, o `V4`/`V5`/`V6` separadas, a tu criterio. Cada tabla necesita como mínimo `id` (PK) y `nombre` (o el campo distintivo correspondiente), siguiendo el mismo estilo de columnas de auditoría (`created_at`/`updated_at`) que ya usa `libros`.
2. **Las 4 capas completas** (`Entity`, `Repository`, `Service`/`ServiceImpl`, `Controller`, `DTO`s de request/response) para `Editorial`, `Idioma` y `EstadoLibro`, siguiendo el patrón ya usado en `Libro` — usa `backend/src/main/java/ec/edu/uteq/pfcbackend/{entity,repository,service,controller,dto}/Libro*` como plantilla literal: mismo estilo de nombres en español, mismo manejo de excepciones (`ResourceNotFoundException`/`BusinessException`), paginación vía `Pageable` en los listados.
3. **Migrar los campos de texto libre de `Libro` a foreign keys reales**: una vez existan las 3 tablas maestras, cambia `Libro.editorial`/`Libro.idioma`/`Libro.estado` (hoy `String`) a relaciones `@ManyToOne` hacia `Editorial`/`Idioma`/`EstadoLibro` (columnas `editorial_id`, `idioma_id`, `estado_id`), vía una migración Flyway nueva adicional (no reescribas `V3`, esa ya está aplicada). Esto va a requerir tocar `LibroRequest`/`LibroResponse` y `LibroServiceImpl` para resolver las referencias por id en vez de guardar texto suelto.
4. **Seeder de datos semilla con ≥50 registros**, como migración Flyway nueva (`V_datos_semilla.sql`, siguiente número disponible en la secuencia — hoy la secuencia va V1 → V2 → V3, así que el seeder va después de tus migraciones de tablas maestras y del paso 3, con el siguiente número libre). El seeder debe poblar **tanto `libros` como las 3 tablas maestras**, y los datos deben ser coherentes entre sí (los `editorial_id`/`idioma_id`/`estado_id` de los libros sembrados deben apuntar a filas que el mismo seeder haya insertado en `editoriales`/`idiomas`/`estados_libro`).

### Compañero 2: frontend Angular + testing + benchmark de cache

- Construir el frontend en Angular 17+ consumiendo esta API REST. **Punto crítico**: la API ya está protegida con JWT — todo lo que esté bajo `/api/**` excepto `/api/auth/**` requiere estar autenticado (cookie `access_token` válida). El frontend necesita implementar el flujo de login primero (`POST /api/auth/login`) antes de poder consumir cualquier otro endpoint; sin eso, cualquier llamada a `/api/libros` (o a las entidades nuevas del Compañero 1) devuelve 401. La cookie es HttpOnly, así que el navegador la maneja solo — Angular no necesita leer ni guardar el token manualmente, solo asegurarse de que las peticiones HTTP incluyan credenciales (`withCredentials: true` en `HttpClient`).
- Escribir tests JUnit del `Repository` con cobertura ≥70%.
- Ejecutar el benchmark de cache: 10 repeticiones de la consulta principal (`GET /api/libros`) con y sin cache, calcular el speedup `S = Tsin/Tcon`.

## 5. Convenciones de commit

- Mensajes descriptivos, en español, en modo imperativo (`"agrega X"`, `"corrige Y"`), como ya se viene haciendo en el historial.
- **Sin trailers de coautoría de IA ni menciones de herramientas de IA**, ni en el mensaje del commit ni en el autor (`git config user.name`/`user.email` debe ser siempre una persona real del equipo). El equipo prefiere que el historial de commits refleje autoría humana real — si usas un asistente de IA para escribir código, el commit lo firmas tú, no la herramienta.

## 6. Convención de ramas (sugerida, no impuesta)

El equipo puede decidir libremente entre trabajar directo sobre `main` o usar Pull Requests. Como referencia, el Anexo A de la guía menciona el patrón `feature/nombre-corto` por persona/tarea — por ejemplo `feature/data-layer` para el trabajo del Compañero 1. Es solo una sugerencia de nomenclatura, no una obligación del equipo.

## 7. Puntos de atención técnica (para no romper cosas)

- **Serialización de `Page`/Jackson en el cache**: `PageImpl` de Spring Data (y sus campos internos `Pageable`/`Sort`) no tienen constructores compatibles con Jackson, así que no se pueden cachear directamente en Redis con `GenericJackson2JsonRedisSerializer` — falla al deserializar. La solución ya implementada es `backend/src/main/java/ec/edu/uteq/pfcbackend/config/CacheablePage.java`, una subclase de `PageImpl` con constructor `@JsonCreator` que oculta `getPageable()`/`getSort()` de la serialización. **Si vas a cachear otro listado paginado nuevo (por ejemplo, de una entidad del Compañero 1), reutiliza `CacheablePage` en vez de devolver un `Page` construido con `.map()` directamente** — si no, vas a reintroducir el mismo error de deserialización.
- **Puerto de Postgres**: el contenedor expone `5433` en el host (no el `5432` estándar) porque en la máquina de desarrollo original ya había otro proyecto usando `5432`. Puertas adentro del contenedor sigue siendo `5432`; solo el mapeo externo cambia (ver `POSTGRES_PORT` en `.env` y `docker-compose.yml`). Si tu máquina no tiene ese conflicto, igual usa `5433` para mantener consistencia con el resto del equipo.
- **TTL de cache**: 5 minutos por defecto (`RedisCacheConfig`). Es intencional y configurable, pero si vas a medir el benchmark de speedup, ten en cuenta que el cache expira solo a los 5 minutos — no hace falta invalidarlo manualmente entre repeticiones si las 10 corridas se hacen en menos de ese tiempo.
- **`ddl-auto: validate`**: el esquema de base de datos se gestiona **exclusivamente por Flyway**, nunca por Hibernate. Si necesitas cambiar una tabla (agregar columna, cambiar tipo, etc.), creas una migración Flyway nueva (`V<n>__descripcion.sql`); **nunca** modifiques una entidad esperando que Hibernate actualice el esquema solo — con `validate`, Hibernate falla al arrancar si la entidad y la tabla no coinciden, en vez de alterarla silenciosamente. Esto es deliberado: evita que el esquema real diverja de lo que está versionado en `db/migration/`.
