# ADR-005: Selección de Spring Boot como framework de backend

## Estado

Aceptado

## Contexto

La sección 5.1 del PDF de la guía exige justificar documentadamente la elección del framework de backend "con referencia al ADR-001 escrito en PE-U3". El ADR-001 de este repositorio ([ADR-001-arquitectura-en-capas.md](ADR-001-arquitectura-en-capas.md)) documenta la arquitectura en capas (controller/service/repository), no la elección del framework en sí — ese ADR asume Spring Boot como dado, sin justificar por qué se descartaron las otras dos opciones que el propio PDF menciona explícitamente como válidas para este ejercicio: **Laravel 11.x** (PHP) y **ASP.NET Core 8.x** (C#). Este ADR-005 llena ese vacío retroactivamente, con la ventaja de que ahora existe evidencia real de cómo se comportó Spring Boot en un proyecto completo (5 recursos CRUD, JWT+roles, cache-aside con Redis, integración con servicio externo, SOAP, 36 tests, Docker Compose de 5 servicios) para contrastar contra lo que habría implicado cada alternativa.

### Criterios de comparación

| Criterio | Spring Boot 3.5.x / Java 21 | Laravel 11.x / PHP 8.3 | ASP.NET Core 8.x / C# |
|---|---|---|---|
| Paradigma dominante | Explícito: cada bean, cada dependencia y cada mapeo se declara (anotaciones + configuración Java), sin "magia" implícita de convención sobre configuración más allá de Spring Boot autoconfiguration | Convención sobre configuración: Eloquent, rutas, migraciones y estructura de carpetas siguen convenciones fuertes con menos código explícito por feature | Punto intermedio: convenciones de ASP.NET Core (Minimal APIs, Controllers) más explícitas que Laravel pero con menos ceremonia que Spring |
| ORM incluido | Spring Data JPA/Hibernate — no es parte del core de Spring Boot, se agrega como starter (`spring-boot-starter-data-jpa`), como se hizo en este proyecto (ver [ADR-004](ADR-004-hibernate-jpa-vs-alternativas.md)) | Eloquent — incluido y fuertemente integrado desde el primer momento, es la opción por defecto del framework | Entity Framework Core — incluido en el ecosistema .NET, comparable en madurez a JPA/Hibernate |
| Motor de plantillas server-side | Thymeleaf (no usado en este proyecto: la UI es Angular vía API REST, no server-side rendering) | Blade — motor de plantillas propio, muy usado en Laravel para SSR clásico | Razor — motor de plantillas de Microsoft, integrado en MVC/Razor Pages |
| Ecosistema de testing | JUnit 5 + Mockito + Testcontainers, maduro y con soporte de primera clase en Spring Boot (`spring-boot-starter-test` trae todo integrado); ya en uso real en este proyecto (`LibroRepositoryTest` con Testcontainers de PostgreSQL) | PHPUnit + Pest, maduro dentro del ecosistema PHP, con buen soporte de Laravel para feature tests HTTP | xUnit/NUnit + `WebApplicationFactory`, maduro dentro de .NET |
| Rendimiento (TechEmpower Framework Benchmarks, Round 23, feb. 2025 — última ronda publicada antes de que el proyecto fuera archivado en marzo 2026, prueba "Fortunes": consulta a BD + orden + render de plantilla, el escenario más parecido a una app real) | 4º lugar, ~243 639 req/s | 8º lugar, ~16 800 req/s | 1er lugar, ~609 966 req/s |
| Adopción en el mercado laboral ecuatoriano/latinoamericano | Percepción informada del equipo (sin dato estadístico duro verificado): Java/Spring tiene presencia consolidada de larga data en el sector empresarial y bancario ecuatoriano, con alta oferta de vacantes backend | PHP/Laravel con presencia real, especialmente en desarrollo web de agencias y freelance | .NET con presencia real pero percibida por el equipo como menor que Java en el segmento backend/empresarial local — esta fila es juicio informado del equipo, no una cifra citable |

## Decisión

Se usa **Spring Boot 3.5.x sobre Java 21** para todo el backend de este proyecto, por:

1. **Dominio previo del equipo con Java/Spring** — el equipo (3 personas) ya tenía experiencia previa con Java desde cursos anteriores de la carrera, mientras que la exposición a PHP/Laravel o C#/.NET era nula o marginal. Para un ejercicio académico con tiempo acotado, arrancar sobre una base de conocimiento real reduce el riesgo de que el tiempo se vaya en aprender el framework en vez de resolver los requisitos del PDF/SGA.
2. **Tipado estático** — para un backend con 6 entidades relacionadas (`Libro`↔`Autor` N:M, `Libro`→`Editorial`/`Idioma`/`EstadoLibro`), reglas de autorización por rol (`@PreAuthorize("hasRole('ADMIN')")`), y un DTO layer explícito (`LibroRequest`/`LibroResponse`/`LibroEnriquecidoResponse`), el chequeo en tiempo de compilación de Java atrapa en el IDE errores que en un lenguaje de tipado dinámico solo aparecerían en tiempo de ejecución (o en producción, si el test no cubre ese camino exacto).
3. **Ecosistema de testing maduro, ya en uso real en este mismo repo** — Testcontainers levantando un PostgreSQL real en `LibroRepositoryTest` (no un mock ni una BD en memoria que se comporte distinto a producción), integrado de forma nativa con JUnit 5 vía `spring-boot-starter-test`, sin configuración adicional de infraestructura de testing.
4. **Madurez del stack de seguridad para lo que este proyecto necesitaba** — Spring Security cubrió, con librerías de primera parte y sin reinventar nada crítico, exactamente los tres mecanismos que este backend requería: autenticación JWT (cookie `HttpOnly` + `Authorization: Bearer` dual, ver `JwtAuthenticationFilter`), autorización declarativa por rol a nivel de método (`@PreAuthorize` + `@EnableMethodSecurity`), y una blacklist de tokens revocados en logout sobre Redis (`TokenBlacklistService`, ver [ADR-003](ADR-003-redis-cache-y-blacklist-jwt.md)) — un patrón funcionalmente equivalente a la revocación de tokens de un flujo OAuth2, sin necesitar un Authorization Server completo para el alcance de este ejercicio.

## Consecuencias

**Positivas:**
- Productividad real aprovechando conocimiento previo del equipo, en vez de invertir tiempo del PFC en aprender un stack nuevo desde cero.
- Tipado estático detectó errores de compilación en varios puntos de este mismo repo antes de llegar a runtime (ver el caso documentado en [ADR-004](ADR-004-hibernate-jpa-vs-alternativas.md): el desajuste de tipos entre `String` y las relaciones `@ManyToOne` se detectó al compilar los tests, no en producción).
- El ecosistema de Spring Boot starters (`spring-boot-starter-data-redis`, `spring-boot-starter-web-services`, `spring-boot-starter-security`, `springdoc-openapi`) permitió cubrir cache-aside, SOAP y OpenAPI con configuración mínima y sin evaluar librerías de terceros no oficiales para cada necesidad.

**Negativas (honestas):**
- **Verbosidad real de Java frente a lo que habría sido Laravel/PHP para el mismo CRUD**: cada recurso de este proyecto (Libro, Autor, Editorial, Idioma, EstadoLibro) requirió Entity + Repository + Service + ServiceImpl + Request DTO + Response DTO + Controller — 7 archivos por recurso. El equivalente en Laravel (Eloquent Model + Controller + Form Request + API Resource) suele resolverse con menos archivos y menos código boilerplate, a costa de menos garantías en tiempo de compilación.
- **Curva de aprendizaje de Spring para quien no lo conocía antes**: no los tres integrantes partían del mismo nivel de familiaridad con Spring específicamente (distinto de "Java" en general) — conceptos como inyección de dependencias vía anotaciones, el ciclo de vida de un bean, o por qué `@Transactional` solo funciona sobre un proxy de Spring y no si se llama al método desde dentro de la misma clase, no son obvios sin experiencia previa puntual con el framework.
- **Configuración inicial más pesada que un microframework**: levantar el proyecto (Postgres, Redis, Flyway, Spring Security, OpenAPI, Spring-WS) implicó más piezas de configuración explícita que un equivalente mínimo en un framework más opinado, aunque el docker-compose final (ver handoff de la Unidad IV) reduce ese costo a un solo comando para cualquiera que clone el repo.

## Alternativas consideradas

- **Laravel 11.x**: descartado principalmente por falta de experiencia previa real del equipo con PHP/Laravel (frente a la base ya construida con Java), no por una deficiencia técnica del framework en sí — Laravel es una opción legítima y madura para este tipo de proyecto, y de hecho el propio PDF de la guía está escrito pensando en Laravel como framework de referencia (ver tabla de equivalencias en `docs/handoff/DECISIONES-GUIA-VS-ENUNCIADO-U4.md`). El tipado dinámico de PHP también se consideró mayor riesgo relativo para las reglas de autorización por rol de este proyecto, donde un error de tipo en un DTO solo se habría detectado en tiempo de ejecución.
- **ASP.NET Core 8.x**: descartado pese a ser, con evidencia real de TechEmpower Round 23, la opción de mayor rendimiento bruto de las tres (~609 966 req/s frente a ~243 639 req/s de Spring en la prueba Fortunes) — el rendimiento no era la restricción activa de este proyecto (el propio benchmark de este mismo repo, `docs/informe/apache-bench.md`, no encontró cuellos de botella con 20 usuarios concurrentes). Se descartó por la misma razón que Laravel: el equipo no tenía experiencia previa real con C#/.NET, y la oferta laboral percibida en el mercado local para .NET backend se consideró menor que para Java, aunque esto último es un juicio informado del equipo, no un dato estadístico verificado.

## Nota de contexto

Este ejercicio usa el dominio de gestión bibliotecaria como caso de práctica de la Unidad III de Aplicaciones Web, independiente del repositorio del Proyecto Fin de Curso real del autor.
