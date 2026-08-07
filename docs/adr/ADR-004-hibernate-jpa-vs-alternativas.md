# ADR-004: Hibernate/JPA para el acceso a datos vs. alternativas

## Estado

Aceptado

## Contexto

El backend usa Spring Data JPA (con Hibernate como implementación) para el acceso a datos de las 5 entidades del catálogo (`Libro`, `Editorial`, `Idioma`, `EstadoLibro`, `Usuario`), con relaciones reales mapeadas vía anotaciones (`Libro` tiene `@ManyToOne` hacia `Editorial`, `Idioma` y `EstadoLibro`, e `@GeneratedValue(strategy = GenerationType.IDENTITY)` en su clave primaria). Esta decisión se compara contra dos alternativas reales disponibles en el mismo stack (Java/Spring Boot):

1. **Hibernate/JPA vs. JDBC plano (`JdbcTemplate`)**: JDBC da control total sobre el SQL exacto ejecutado y cero "magia" de generación de queries, a cambio de escribir manualmente el mapeo `ResultSet` → objeto Java para cada entidad, sin cache de primer nivel ni gestión automática de relaciones (`@ManyToOne` tendría que resolverse con JOINs manuales o queries N+1 explícitas). Para un catálogo con relaciones reales (`Libro` → `Editorial`/`Idioma`/`EstadoLibro`), el costo de mantenimiento del mapeo manual crece con cada entidad nueva.

2. **Hibernate/JPA vs. MyBatis**: MyBatis es un punto intermedio — SQL explícito en XML o anotaciones, pero con mapeo automático de resultados a objetos. Da más control que JPA sobre el SQL exacto (relevante para optimizar queries complejas) a cambio de perder las relaciones automáticas vía anotaciones y el dirty checking de Hibernate (detección automática de cambios en una entidad gestionada para generar el `UPDATE` sin llamar a `save()` explícitamente).

**Flyway ya gestiona el esquema de forma explícita**: el proyecto tiene 6 migraciones Flyway versionadas (`V1__crear_tabla_productos.sql` a `V6__datos_semilla.sql`), y `spring.jpa.hibernate.ddl-auto` está configurado en `validate` (`application.yml`), no en `create`/`update`. Esto mitiga la principal crítica a JPA/Hibernate (que "esconde" el DDL): aquí Hibernate solo valida en el arranque que el mapeo de las entidades coincide con el esquema que Flyway ya creó, y hace el mapeo objeto-relacional en tiempo de ejecución — no genera ni modifica el esquema.

## Decisión

Se usa Spring Data JPA con Hibernate para las 5 entidades del catálogo, con Flyway gestionando el DDL de forma independiente y versionada.

## Consecuencias

**Positivas:** productividad (CRUD básico sin SQL manual vía Spring Data JPA repositories), relaciones `@ManyToOne` declarativas, dirty checking, portabilidad entre motores SQL si se necesitara cambiar de PostgreSQL a otro RDBMS.

**Negativas:** curva de aprendizaje del comportamiento de sesión/lazy loading de Hibernate, riesgo real de queries N+1 si no se es explícito con fetch joins, y un caso real ya vivido en este mismo repo: el mapeo objeto-relacional puede ocultar en tiempo de escritura del test que un campo dejó de ser `String` para convertirse en una relación — los tests de `LibroRepositoryTest` y `LibroServiceImplTest` fueron escritos asumiendo `editorial`/`idioma`/`estado` como `String` en el builder de `Libro`, cuando la entidad ya había evolucionado a relaciones `@ManyToOne` hacia `Editorial`/`Idioma`/`EstadoLibro`; el desajuste solo se detectó al compilar los tests (evidencia concreta: commit `1db839b`).

## Riesgo de queries N+1 (honesto, sin mitigar en el alcance actual)

Con las relaciones `@ManyToOne` actuales (`Libro` → `Editorial`/`Idioma`/`EstadoLibro`), listar N libros puede generar N+1 queries si no se usa `@EntityGraph` o `JOIN FETCH` explícito: una query para el listado paginado más una query adicional por cada libro para resolver cada relación perezosa. Esto no fue medido ni mitigado en el alcance actual del PFC académico — es una deuda técnica documentada, no una omisión oculta. El cache-aside de Redis (ADR-003) mitiga parcialmente el impacto en el endpoint de listado más usado, pero no resuelve el problema de fondo si se agregan más relaciones a futuro.

## Alternativas consideradas

- **JDBC plano (`JdbcTemplate`)**: descartado por el costo de mantenimiento manual del mapeo objeto-relacional a medida que crecen las relaciones entre entidades.
- **MyBatis**: descartado porque el equipo prioriza velocidad de desarrollo del CRUD básico sobre control fino del SQL, dado que las queries del catálogo no son complejas en esta etapa del proyecto.

## Nota de contexto

Este ejercicio usa el dominio de gestión bibliotecaria como caso de práctica de la Unidad III de Aplicaciones Web, independiente del repositorio del Proyecto Fin de Curso real del autor.
