# Fundamento Teórico — Unidad III (Aplicaciones Web)

Sistema PFC — Gestión Bibliotecaria. Cada sección integra evidencia real de este mismo repositorio (código, ADR, migraciones, benchmark ya ejecutado) junto con la base teórica que la sustenta.

## 5.1 Gestión de Estado en Aplicaciones Web

El protocolo HTTP es, por diseño, stateless: cada petición se procesa de forma completamente independiente de las anteriores, sin que el servidor conserve memoria de interacciones previas del mismo cliente entre una petición y la siguiente (Fielding & Reschke, 2014). Esta característica, heredada de la arquitectura REST descrita originalmente por Fielding (2000) en su disertación doctoral, es precisamente lo que permite que cualquier servidor pueda atender cualquier petición entrante sin coordinación previa con otro servidor: es la base que hace posible el balanceo de carga horizontal sin pegajosidad de sesión (*sticky sessions*). El costo de esa ventaja es que toda aplicación con estado de usuario debe reintroducir ese estado por algún mecanismo externo al protocolo mismo: cookies (el navegador las reenvía automáticamente al mismo dominio), sesiones de servidor (un identificador en cookie referencia un estado guardado server-side), JWT stateless (el propio token, firmado, contiene el estado — el servidor no guarda nada, solo verifica la firma) y bases de datos (el estado persistente de negocio).

| Mecanismo | Capacidad | Persistencia | Accesible desde JS | Envío automático | Riesgo principal |
|---|---|---|---|---|---|
| localStorage | ~5-10 MB | Indefinida | Sí | No | XSS: cualquier script inyectado lee el token |
| sessionStorage | ~5-10 MB | Solo la pestaña | Sí | No | XSS (ventana de exposición menor) |
| Cookie (`HttpOnly`) | ~4 KB | Configurable | No (si `HttpOnly`) | Sí, automático | CSRF, mitigable con `SameSite` |

Para este proyecto la elección correcta es la cookie `HttpOnly` + `SameSite=Strict`, ya implementada en `AuthController` (`backend/src/main/java/.../controller/AuthController.java`): el JWT nunca se expone a JavaScript, eliminando el vector de robo por XSS que sí existe con `localStorage`.

```java
private ResponseCookie construirCookie(String valor, Duration maxAge) {
    return ResponseCookie.from(COOKIE_NAME, valor)
            .httpOnly(true).sameSite("Strict").path("/").maxAge(maxAge).build();
}
```

El problema clásico de escalar sesiones de servidor es que, si el estado vive en memoria de una sola instancia, el balanceador debe enrutar siempre al mismo cliente hacia esa instancia (*sticky session*), anulando buena parte del beneficio del balanceo. La solución es centralizar el estado: sistema de archivos (rápido localmente, no accesible entre instancias, sin TTL nativo), BD relacional (centralizada y durable, pero exige job de limpieza y es más lenta por I/O a disco) o Redis (centralizado, en memoria, atómico vía `SET ... EX`, TTL nativo). Este proyecto usa Redis con ese propósito, aunque no para sesiones sino para la blacklist de JTI que invalida JWT en logout (ADR-003): la limitación estructural del JWT es que un token firmado no se puede revocar por diseño, así que esa blacklist centralizada con TTL automático es la solución real al problema teórico de invalidación.

El ciclo Request→Response para una petición autenticada pasa primero por `JwtAuthenticationFilter`, que intercepta antes que cualquier controller:

```java
if (!tokenBlacklistService.estaEnBlacklist(claims.getId())) {
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("rol", String.class)));
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
}
```

Solo si esa validación es exitosa la petición llega al controller correspondiente, sin que el servidor haya guardado sesión alguna entre esta petición y la siguiente.

## 5.2 ORM y Mapeo Objeto-Relacional

El *impedance mismatch* es el desajuste estructural entre el modelo orientado a objetos —grafos de objetos con identidad, encapsulación, herencia y referencias directas— y el modelo relacional —tablas planas relacionadas por claves foráneas, sin identidad más allá de la clave primaria (Fowler, 2002). Un ORM como Hibernate/JPA traduce automáticamente entre ambos mundos: convierte un grafo de objetos Java en inserts/updates SQL, y filas de resultado en objetos reconstruidos.

Cuando el dominio necesita herencia (algo que este proyecto no tiene, pero es parte del temario), un ORM ofrece tres estrategias: *Single Table* (una tabla para toda la jerarquía con columna discriminadora; lecturas rápidas sin JOIN, pero muchas columnas nulas), *Class Table*/*Joined* (una tabla por clase, normalizada, con JOIN para reconstruir la instancia) y *Concrete Table* (una tabla por clase concreta, sin tabla compartida, sin soporte natural para consultas polimórficas). Si este proyecto necesitara distinguir, por ejemplo, `LibroFisico` de `LibroDigital`, la estrategia `JOINED` sería la más coherente con las decisiones ya tomadas: mantiene el esquema normalizado (consistente con Flyway gestionando DDL explícito) a costa de JOINs adicionales, costo aceptable con el volumen actual (54 libros).

| Criterio | ORM (JPA/Hibernate) | SQL puro (`JdbcTemplate`) | Query Builder |
|---|---|---|---|
| Curva de aprendizaje | Alta | Baja-media | Media |
| Rendimiento en consultas simples | Bueno | Óptimo | Bueno |
| Rendimiento en consultas complejas | Puede degradarse sin tuning | Óptimo | Bueno |
| Problema N+1 | Riesgo alto si no se es explícito | No aplica | Riesgo bajo si es deliberado |
| Portabilidad entre motores | Alta | Baja | Media |
| Debugging de queries | Difícil (SQL generado) | Fácil (SQL visible) | Medio |

El problema N+1 es el caso donde la comodidad del ORM cobra su precio: al listar N libros con relaciones `@ManyToOne` hacia `Editorial`, `Idioma` y `EstadoLibro`, Hibernate puede ejecutar una query por el listado más una adicional por cada libro, si no se usa `@EntityGraph` o `JOIN FETCH` explícito. Este riesgo está documentado honestamente en **ADR-004** como deuda técnica no mitigada en el alcance actual: el cache-aside de Redis solo mitiga parcialmente el impacto en el listado, no el problema de fondo.

Sobre gestión del esquema: Flyway (migraciones SQL versionadas explícitas), `hbm2ddl.auto` (generación automática desde las entidades, cómoda para prototipar pero riesgosa en producción porque `create`/`update` puede alterar datos de forma impredecible) y Liquibase (changelogs XML/YAML/JSON, mayor abstracción de formato). Este proyecto usa Flyway con `spring.jpa.hibernate.ddl-auto: validate` (confirmado en `application.yml`, citado en ADR-004): Hibernate nunca genera el esquema, solo valida que el mapeo coincide con el DDL que Flyway ya aplicó de forma versionada.

## 5.3 Patrones de Arquitectura para Aplicaciones Web Escalables

El modelo C4 de Simon Brown organiza la documentación de arquitectura en cuatro niveles de zoom —Contexto (sistema y actores externos), Contenedores (unidades desplegables), Componentes (piezas internas de un contenedor) y Código (diagramas de clases UML, el más detallado)—, cada uno dirigido a una audiencia distinta (Brown, s.f.). Esto lo hace superior a un UML de alto nivel aislado, que suele mezclar niveles de abstracción: un stakeholder no técnico entiende el diagrama de Contexto sin necesitar el de Componentes, mientras un desarrollador nuevo va del Contexto al Componente exacto sin perderse. Este proyecto ya implementa los primeros tres niveles en `docs/arquitectura/workspace.dsl`: Contexto, Contenedores (Frontend Angular, Backend Spring Boot, PostgreSQL, Redis) y Componentes (9 componentes internos del backend).

Sobre capas vs microservicios, la decisión ya está documentada en **ADR-001**: para un equipo de 3 personas, la *team cognitive load* es el factor decisivo — repartir el trabajo entre microservicios no reduce la complejidad total, la multiplica en superficie de coordinación (contratos, versionado de APIs internas, consistencia eventual) sin que el equipo tenga tamaño para paralelizarlo. A eso se suman las falacias de los sistemas distribuidos (Deutsch, 1994, como se citó en Rotem-Gal-Oz, 2006): microservicios asumen red confiable, latencia cero y topología estable, y cada suposición violada introduce un modo de fallo nuevo que hay que diseñar y testear. Para un catálogo sin fronteras de dominio naturales ni necesidad de escalar partes independientemente, ese costo no tiene contrapartida.

Event-Driven Architecture (EDA) y WebSockets se justifican cuando existe necesidad real de actualizaciones bidireccionales en tiempo real (chat, notificaciones en vivo) o de desacoplar productores y consumidores a mayor escala. Honestamente, este proyecto no tiene ese requisito: ningún cliente necesita enterarse en tiempo real de cambios de stock, y REST síncrono ya cubre la necesidad con latencia predecible. Un broker de eventos añadiría complejidad operativa (consistencia eventual, reintentos) sin caso de negocio que lo justifique hoy.

Finalmente, los Architecture Decision Records son una práctica de gobernanza reconocida por el SWEBOK v4.0 (IEEE Computer Society, 2024) dentro de su área de conocimiento de Arquitectura de Software, para preservar el razonamiento detrás de decisiones y permitir su reevaluación futura, siguiendo el formato ligero de Nygard (2011): Título, Estado, Contexto, Decisión, Consecuencias. Este repositorio ya tiene cuatro ADR completos (001-004) con ese formato exacto.

## 5.4 Caché: Patrones y Medición de Rendimiento

Una aplicación web tiene caché en varios niveles: el navegador (`Cache-Control`/`ETag`, apropiado para assets estáticos, invalidado por versión de archivo), la CDN (caché de borde, invalidada por purga o TTL corto), la aplicación (Redis aquí, TTL 5 min, invalidación explícita vía `@CacheEvict`) y la base de datos (buffer pool interno, gestionado automáticamente).

Existen cuatro patrones de caché: *cache-aside* (la app consulta el caché primero; si falla, consulta la BD, puebla el caché y responde; en escritura, escribe en BD y luego invalida el caché), *read-through* (el caché es transparente delante de la BD; la app siempre le habla al caché), *write-through* (cada escritura va primero al caché, que sincrónicamente la propaga a la BD) y *write-behind* (la escritura se confirma en caché y se propaga asíncronamente, con riesgo de pérdida de datos). Este proyecto usa cache-aside (`@Cacheable`/`@CacheEvict` con `allEntries=true` en escrituras), justificado en **ADR-003** porque el catálogo tiene lecturas mucho más frecuentes que escrituras, y cache-aside da control explícito sobre cuándo leer e invalidar sin forzar que cada escritura pase por el caché.

El *cache stampede* ocurre cuando muchas peticiones simultáneas fallan contra la misma key y todas golpean la BD a la vez; se mitiga con mutex (solo una petición recalcula, las demás esperan) o *probabilistic early expiration* (recalcular antes de expirar, con probabilidad creciente). **ADR-003 documenta honestamente este riesgo como no mitigado** en el alcance actual: ni `@Cacheable(sync=true)` ni invalidación selectiva por página están implementados, decisión consciente dado el volumen de tráfico esperado.

Redis ofrece strings, hashes, sets y sorted sets, pero este proyecto solo usa *strings con TTL*: el listado cacheado se serializa como JSON (`GenericJackson2JsonRedisSerializer`) y la blacklist de JTI también es un string por key. Las estructuras más ricas quedan disponibles para evolucionar el caché sin cambiar de motor.

La medición de rendimiento debe ser estadísticamente válida: muestra de tamaño razonable (n≥10), media y desviación estándar, e idealmente un intervalo de confianza. Este proyecto ejecutó ese benchmark real (`docs/benchmark-cache.md`): en régimen estable, el speedup medido es **1.70x** con IC 95% **(1.50x, 1.90x)**, y el P95 pasa de **16.98 ms** sin caché a **10.05 ms** con caché — evidencia real, no una cifra de manual.

## 5.5 Análisis de Escalabilidad Horizontal

Si el PFC tuviera 10 000 usuarios concurrentes, escalar horizontalmente requeriría actuar sobre cuatro frentes, conectados con el C4 ya documentado (`docs/arquitectura/workspace.dsl`):

**1. Balanceador de carga.** Un reverse proxy (Nginx) se ubicaría delante de los contenedores del Nivel 2, recibiendo el tráfico externo y distribuyéndolo entre instancias del backend (round-robin o least-connections), además de terminar TLS — sustituiría la relación `frontend -> backend` por `frontend -> Nginx -> [backend×N]`.

**2. Múltiples instancias del backend.** Esto ya es posible hoy porque el backend es *stateless por diseño*: no hay sesión HTTP de servidor, el JWT viaja en cookie y cualquier instancia lo valida de forma independiente con el mismo secreto compartido. Lo que lo bloquearía si no fuera así: sesiones en memoria local forzarían *sticky sessions*, atando cada cliente a una instancia y anulando el beneficio del balanceo.

**3. Sesiones centralizadas en Redis, sin sticky sessions.** Conectando con 5.1: este proyecto ya centraliza en Redis un estado compartido entre instancias —la blacklist de JTI— que cualquier instancia lee y escribe sin depender de que sea siempre la misma la que atienda a un cliente dado. Si se necesitara sesión de servidor real, el mismo patrón es directamente extensible.

**4. Base de datos con réplica de lectura.** El catálogo tiene lecturas mucho más frecuentes que escrituras (la misma premisa de ADR-003), lo que hace natural dirigir `GET /api/libros` hacia una réplica de lectura de PostgreSQL, dejando el primario solo para escrituras. El costo es consistencia eventual: una ventana breve donde un libro recién creado podría no ser visible aún en la réplica. Para un catálogo bibliotecario ese trade-off es aceptable, y se reduce más gracias al caché ya existente.

**Lo que no está resuelto hoy.** El cache stampede de ADR-003 se agravaría con más instancias: si varias compiten por la misma key en un miss simultáneo, cada una golpearía Postgres a la vez, multiplicando el problema por el número de instancias en vez de resolverlo. Tampoco existe gestión centralizada de secretos (`jwt.secret` vía variable de entorno, replicado manualmente por instancia) ni rate limiting o circuit breaker entre backend y Redis/Postgres. Escalar sin abordar esto primero solo movería el cuello de botella.

## Referencias

Bass, L., Clements, P., & Kazman, R. (2021). *Software architecture in practice* (4.ª ed.). Addison-Wesley.

Brown, S. (s.f.). *The C4 model for visualising software architecture*. Recuperado el 6 de agosto de 2026, de https://c4model.com/

Fielding, R. T. (2000). *Architectural styles and the design of network-based software architectures* [Tesis doctoral, University of California, Irvine]. https://ics.uci.edu/~fielding/pubs/dissertation/top.htm

Fielding, R., & Reschke, J. (Eds.). (2014). *Hypertext Transfer Protocol (HTTP/1.1): Message syntax and routing* (RFC 7230). Internet Engineering Task Force. https://www.rfc-editor.org/rfc/rfc7230

Fowler, M. (2002). *Patterns of enterprise application architecture*. Addison-Wesley.

IEEE Computer Society. (2024). *Guide to the software engineering body of knowledge (SWEBOK), versión 4.0* (H. Washizaki, Ed.). https://www.computer.org/education/bodies-of-knowledge/software-engineering

Nygard, M. T. (2011, 15 de noviembre). *Documenting architecture decisions*. Cognitect Blog. https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions

Nygard, M. T. (2018). *Release it!: Design and deploy production-ready software* (2.ª ed.). Pragmatic Bookshelf.

Rotem-Gal-Oz, A. (2006). *Fallacies of distributed computing explained* [documento formal que recoge las ocho falacias originadas por Peter Deutsch en 1994 y ampliadas por James Gosling en 1997; la lista original nunca fue publicada formalmente por sus autores]. https://arnon.me/wp-content/uploads/Files/fallacies.pdf

**Nota de verificación bibliográfica:** las nueve referencias anteriores fueron verificadas activamente durante la redacción de este documento (búsqueda web para confirmar edición, año y fuente exacta de cada una, incluyendo la existencia real y fecha de publicación de SWEBOK v4.0 y el origen no formalmente publicado de las falacias de Deutsch) — ninguna es una cifra o dato bibliográfico asumido de memoria sin contrastar.
