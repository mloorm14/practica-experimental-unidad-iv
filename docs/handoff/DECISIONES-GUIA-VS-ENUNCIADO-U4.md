# Análisis de incoherencias: Guía de Práctica vs. Enunciado de Tarea — Unidad IV

Este documento registra las discrepancias detectadas entre el PDF de la guía de práctica (Unidad IV) y el enunciado/SGA de la tarea puntual, y cómo se resolvió cada una en el código real de este repositorio. Regla general aplicada en todas: donde el PDF y el SGA se contradicen directamente, manda el PDF (es el documento base del curso); donde el SGA pide algo adicional que el PDF no menciona, se cumplen ambos requisitos, no se elige uno.

## 1. Path de Swagger UI

**Discrepancia**: el PDF exige que la documentación interactiva viva en `/api/documentation`; el SGA exige `/api/docs`.

**Resuelto**: ambos funcionan. `springdoc.swagger-ui.path: /api/documentation` en `application.yml` sirve la UI real ahí (ver [OpenApiConfig.java](../../backend/src/main/java/ec/edu/uteq/pfcbackend/config/OpenApiConfig.java) para el bean de seguridad/metadata de OpenAPI). `/api/docs` es un alias que responde con una redirección 302 hacia `/api/documentation`, implementado en [DocsRedirectController.java](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/DocsRedirectController.java) — un único `@RestController` con un `@GetMapping("/api/docs")` que devuelve un `RedirectView`. El JSON crudo de OpenAPI queda en el path por defecto de springdoc (`/v3/api-docs`), sin necesidad de fijarlo explícitamente porque ni el PDF ni el SGA exigen un path específico para él, solo para la UI interactiva.

## 2. Norma de citación bibliográfica

**Discrepancia**: el PDF exige norma IEEE; el SGA exige norma APA.

**Resuelto para el informe de la Unidad III**: se usó APA en el cuerpo del documento (`docs/informe/Informe-Tecnico-Unidad-III-Equipo-D.docx`), por ser la norma exigida por el SGA, que es el canal donde efectivamente se calificó la entrega.

**Pendiente de definir para el informe de la Unidad IV**: se recomienda seguir el mismo criterio ya aplicado en la Unidad III (APA), por ser la norma del SGA, que es donde se califica la entrega — no hay razón técnica para cambiar de criterio entre unidades del mismo curso. Ver la asignación de esta tarea en `GUIA-EQUIPO-U4.md`, sección "TODOS — Informe técnico final" (referencias APA, mínimo 5 fuentes).

## 3. El PDF está escrito pensando en Laravel

**Discrepancia**: buena parte de la terminología y de los ejemplos del PDF de la guía asumen un backend en Laravel (Eloquent, Sanctum, Blade, artisan), no Spring Boot — la elección real de framework está justificada en [ADR-005](../adr/ADR-005-seleccion-framework-spring-boot.md).

**Resuelto** con una tabla de equivalencias reales entre lo que el PDF describe en términos de Laravel y lo que efectivamente se implementó en este backend Spring Boot:

| Concepto del PDF (Laravel) | Equivalente real en este proyecto (Spring Boot) |
|---|---|
| Laravel Breeze + Spatie (roles/permisos) | Spring Security + `@PreAuthorize("hasRole('ADMIN')")` (`@EnableMethodSecurity`) |
| Laravel Sanctum (tokens de API) | JWT propio firmado HS384 (`JwtService`), dual cookie `HttpOnly` + header `Authorization: Bearer` |
| L5-Swagger | springdoc-openapi 2.9.0 (`OpenApiConfig`, alias `/api/docs`→`/api/documentation`) |
| GuzzleHTTP (cliente HTTP para servicios externos) | `WebClient` de Spring WebFlux (`WebClientConfig`, usado en `OpenLibraryClient`) |
| `php artisan test` | `./mvnw -B clean verify` (JUnit 5 + Mockito + Testcontainers) |
| MySQL | PostgreSQL 16 |

## 4. Número de recursos CRUD (contradicción interna del propio PDF)

**Discrepancia**: el Objetivo General del PDF pide 5 recursos CRUD; el Objetivo Específico 1 (OE1) pide 4 — el PDF se contradice a sí mismo.

**Resuelto** con 5 recursos: `Libro`, `Autor`, `Editorial`, `Idioma`, `EstadoLibro` — se cumplió el número mayor de los dos exigidos por el mismo documento, en vez de arriesgar no cumplir el Objetivo General por seguir literalmente el OE1.

## 5. SOAP: exigido por el Resultado de Aprendizaje, ambiguo en los objetivos específicos

**Discrepancia**: el Resultado de Aprendizaje del curso exige explícitamente implementar un servicio SOAP; los objetivos específicos del PDF solo piden una comparación teórica SOAP vs. REST, sin exigir una implementación funcional.

**Resuelto cubriendo el escenario de mayor riesgo**: se implementó un servicio SOAP real con Spring-WS, contract-first (`libro-catalogo.xsd` → JAXB), expuesto en `/ws/libro-catalogo.wsdl`, en vez de apostar a que la comparación teórica sola fuera suficiente para el Resultado de Aprendizaje. La comparación teórica también se documentó (`docs/informe/ejemplo-soap-vs-rest.md`), cumpliendo ambos requisitos.

## 6. Herramienta de prueba de carga: Apache Bench vs. k6

**Discrepancia**: ninguna real — el PDF exige explícitamente Apache Bench (`ab`), no deja la herramienta abierta a elección del equipo (k6 hubiera sido una alternativa moderna válida técnicamente, pero no es lo que el PDF pide).

**Resuelto**: se usó `ab` tal como el PDF exige, corrido desde un contenedor Docker descartable (ver `docs/informe/apache-bench.md` para la justificación de por qué no se instaló a nivel de sistema operativo).

## 7. PWA: exigido solo por el SGA, no mencionado en el PDF

**Discrepancia**: ninguna — es un requisito adicional del SGA que el PDF simplemente no cubre (sección de tendencias del informe técnico, mínimo 400 palabras, debe mencionar PWA entre otras).

**Estado**: PENDIENTE. Asignado a Kevin en `GUIA-EQUIPO-U4.md` (`ng add @angular/pwa` + reporte Lighthouse), tanto la implementación real en el frontend como la mención correspondiente en la sección de tendencias del informe.

## 8. Percentiles P50/P95/P99 en la prueba de carga

**Discrepancia**: ninguna — el PDF exige explícitamente reportar P50/P95/P99, no solo throughput promedio.

**Resuelto**: ver `docs/informe/apache-bench.md`, que incluye la tabla de percentiles (`Percentage of the requests served within a certain time`) tal como `ab` la imprime, para las dos corridas (cache frío y cache caliente).
