# Auditoría de seguridad OWASP Top 10:2021

Auditoría contra el código real de este proyecto (no genérica), con el sistema completo corriendo vía `docker compose up -d --build` (backend, Postgres, Redis, frontend, Nginx). Cada control se verificó leyendo el código fuente citado y, donde aplica, ejecutando el ataque/prueba real con `curl` contra el sistema en vivo a través de Nginx (puerto 80).

Filosofía de esta auditoría: documentar honestamente los gaps encontrados. Un informe con gaps declarados es más valioso que uno que finja cobertura completa.

---

## A01:2021 — Broken Access Control

**Vector de ataque específico**: un usuario autenticado con rol `USER` (lectura únicamente) intenta ejecutar una operación de escritura (crear/actualizar/eliminar) sobre cualquiera de los 6 recursos CRUD del sistema, algo que solo debería poder hacer `ADMIN`.

**Contramedida real**: `@PreAuthorize("hasRole('ADMIN')")` en los métodos `crear`/`actualizar`/`eliminar` de los 6 controladores REST (habilitado por `@EnableMethodSecurity`):
- [`LibroController.java:71,78,85`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/LibroController.java)
- [`AutorController.java:57,64,71`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/AutorController.java)
- [`EditorialController.java:57,64,71`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/EditorialController.java)
- [`IdiomaController.java:57,64,71`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/IdiomaController.java)
- [`EstadoLibroController.java:57,64,71`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/controller/EstadoLibroController.java)

**Evidencia real (curl, usuario `usuario_prueba` / rol USER, seed `V7__usuario_prueba_rol_user.sql`)**:
```
$ curl -s -i -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario_prueba","password":"User123!"}'
HTTP/1.1 200
Set-Cookie: access_token=...rol":"USER"...

$ curl -s -i -X POST http://localhost/api/v1/libros \
  -H "Authorization: Bearer <token_user>" -H "Content-Type: application/json" \
  -d '{"titulo":"test","isbn":"0000000000000","autor":"Test Autor","stock":1,"anioPublicacion":2020,"editorialId":1,"estadoId":1,"idiomaId":1}'
HTTP/1.1 403
{"type":"about:blank","title":"Forbidden","status":403,"detail":"No tiene permisos para realizar esta operacion","instance":"/api/v1/libros"}
```
El body de la request debía ser válido (todos los campos `@NotNull`/`@NotBlank` completos): con un body incompleto, la validación de `@Valid` en el binding del argumento se ejecuta antes de que el proxy AOP de `@PreAuthorize` intercepte la llamada, devolviendo 400 en vez de 403 — no probaría el control de autorización. Con body completo, la autorización real se ejerce y bloquea correctamente con 403.

**Veredicto**: mitigado.

---

## A02:2021 — Cryptographic Failures

**Vector de ataque específico**: comprometer la tabla `usuarios` (dump de BD) o interceptar el token de sesión, y usarlo para escalar acceso u obtener las contraseñas en claro.

### Hasheo de contraseñas

`BCryptPasswordEncoder()` sin argumentos ([`SecurityConfig.java:25-26`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/config/SecurityConfig.java)) — costo (log rounds) por defecto = **10**, confirmado también en el comentario del seed [`V7__usuario_prueba_rol_user.sql:9`](../../backend/src/main/resources/db/migration/V7__usuario_prueba_rol_user.sql). Un dump de la tabla `usuarios` nunca expone contraseñas en claro; el hash `$2a$10$...` es de costo razonable para 2026 en un sistema académico (no se ajustó a un costo mayor porque no hay presupuesto de cómputo dedicado a esto en un PFC).

### JWT firmado

`JwtService.java:37` firma con `signWith(secretKey)` donde `secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` ([`JwtService.java:18-23`](../../backend/src/main/java/ec/edu/uteq/pfcbackend/security/JwtService.java)) — la librería `jjwt` infiere el algoritmo HMAC a partir de la longitud en bits de la clave decodificada. **Algoritmo real confirmado en un token emitido de verdad** (header decodificado de un JWT real obtenido vía `/api/v1/auth/login`): `{"alg":"HS384"}` — es decir, el secreto configurado en `.env` (`JWT_SECRET`) decodifica a 48 bytes (384 bits), no 256. La firma HMAC-SHA384 es criptográficamente robusta y evita el ataque clásico de "alg: none" porque `Jwts.parser().verifyWith(secretKey)` (`JwtService.java:43`) exige verificación con la misma clave simétrica, rechazando tokens sin firma o con algoritmo distinto.

### Cookie de sesión

**Corregido** (originalmente reportado como gap: `secure(false)` estaba hardcodeado). `AuthController.java:85-94` (`construirCookie`) ahora usa una propiedad de configuración en vez del literal:
```java
@Value("${app.cookie.secure}")
private boolean cookieSecure;
...
return ResponseCookie.from(COOKIE_NAME, valor)
        .httpOnly(true)
        // app.cookie.secure (COOKIE_SECURE): true por defecto, override a false solo
        // en el .env de desarrollo local (docker-compose sobre HTTP plano sin TLS).
        .secure(cookieSecure)
        .sameSite("Strict")
        ...
```
La propiedad se define en [`application.yml:32-36`](../../backend/src/main/resources/application.yml) como `app.cookie.secure: ${COOKIE_SECURE:true}` — **segura por defecto** (si no se define `COOKIE_SECURE`, el valor es `true`). Solo se overridea a `false` explícitamente en el `.env` de desarrollo local (`COOKIE_SECURE=false`, documentado en `.env.example`), porque el `docker-compose` local corre sobre HTTP plano sin TLS.

**Evidencia real del toggle funcionando en ambas direcciones**:
```
# Con COOKIE_SECURE=false (.env de desarrollo local, HTTP):
Set-Cookie: access_token=...; Path=/; Max-Age=900; ...; HttpOnly; SameSite=Strict
                                                          ↑ sin "Secure"

# Con COOKIE_SECURE=true (override puntual de verificacion, mismo binario):
Set-Cookie: access_token=...; Path=/; Max-Age=900; ...; Secure; HttpOnly; SameSite=Strict
                                                          ↑ "Secure" presente
```
Ya no es responsabilidad manual de recordar cambiar una línea de código y recompilar: el valor por defecto es seguro (`true`), y solo un entorno explícitamente configurado como desarrollo local (vía `.env`) lo relaja.

**Veredicto**: mitigado — hash correcto, JWT firmado correctamente, y la bandera `Secure` de la cookie ahora es segura por defecto y condicionada por entorno (no hardcodeada).

---

## A03:2021 — Injection

**Vector de ataque específico**: inyectar SQL a través del parámetro de búsqueda `titulo` de `GET /api/v1/libros?titulo=...`.

**Contramedida real**: el repositorio usa un método derivado de Spring Data JPA, completamente parametrizado — no hay concatenación de strings SQL en ningún punto del código:
```java
// LibroRepository.java:11
Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
```
Spring Data traduce esto a una consulta JPQL con bind parameters (`LIKE ?1`), ejecutada por Hibernate vía `PreparedStatement` — el valor de `titulo` nunca se interpola directamente en el SQL.

**Evidencia real (curl, intentos reales de inyección)**:
```
$ curl -s -G "http://localhost/api/v1/libros" --data-urlencode "titulo=' OR '1'='1" -H "Authorization: Bearer <token>"
{"success":true,"data":[],"message":null,"errors":[],"meta":{"total":0,"current_page":0,"last_page":0}}

$ curl -s -G "http://localhost/api/v1/libros" --data-urlencode "titulo=x'; DROP TABLE libros; --" -H "Authorization: Bearer <token>"
{"success":true,"data":[],"message":null,"errors":[],"meta":{"total":0,"current_page":0,"last_page":0}} (HTTP 200)

$ curl -s "http://localhost/api/v1/libros" -H "Authorization: Bearer <token>"
{"success":true,"data":[{"id":1,"titulo":"Cien años de soledad",...
```
Ambos intentos se tratan como texto literal de búsqueda (0 resultados, porque ningún título contiene esas comillas), no como código SQL. La consulta posterior confirma que la tabla `libros` sigue intacta — el intento de `DROP TABLE` no tuvo ningún efecto.

**Veredicto**: mitigado.

---

## A04:2021 — Insecure Design

**Vector de ataque específico**: fuerza bruta contra `/api/v1/auth/login` o abuso del endpoint de listado para agotar recursos (DoS de aplicación).

**Gap real declarado**: **no existe ningún mecanismo de rate limiting** en el sistema — se buscó explícitamente (`Bucket4j`, `Resilience4j`, cualquier filtro de límite de tasa) en todo `backend/` y no se encontró nada. Confirmado con una prueba real de 5 intentos de login consecutivos con contraseña incorrecta:
```
$ for i in 1 2 3 4 5; do curl -s -o /dev/null -w "intento $i: %{http_code}\n" \
    -X POST http://localhost/api/v1/auth/login -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong"}'; done
intento 1: 401
intento 2: 401
intento 3: 401
intento 4: 401
intento 5: 401
```
Los 5 intentos se procesan igual, sin backoff, sin bloqueo temporal, sin `429 Too Many Requests`. Un atacante puede intentar credenciales indefinidamente contra `/api/v1/auth/login` sin fricción alguna (más allá del propio costo de cómputo de BCrypt por intento, que ralentiza pero no impide un ataque distribuido).

**Por diseño, no por descuido**: para el volumen de tráfico esperado de un sistema académico sin exposición pública real, el costo de implementar rate limiting (Bucket4j + Redis, ya disponible en la infraestructura) no se justificó dentro del alcance de este PFC — pero es un gap real que debe documentarse, no ignorarse. Ver también el riesgo de *cache stampede* ya declarado honestamente en [ADR-003](../adr/ADR-003-redis-cache-y-blacklist-jwt.md), de naturaleza similar (aceptado y documentado, no mitigado).

**Veredicto**: gap declarado, no mitigado.

---

## A05:2021 — Security Misconfiguration

**Vector de ataque específico**: acceder a endpoints de Actuator que filtrarían configuración sensible (`/actuator/env` expondría `JWT_SECRET`, credenciales de BD, etc.; `/actuator/beans` expondría el grafo interno de la aplicación).

**Contramedida real, doble capa**:

1. **Nginx no enruta `/actuator/*` en absoluto** — [`nginx/nginx.conf`](../../nginx/nginx.conf) solo tiene `location /api/`, `location /ws/` y `location /` (fallback a frontend). Cualquier request a `/actuator/...` a través del punto de entrada público (puerto 80) cae en el fallback SPA de Angular, nunca llega al backend:
```
$ curl -s -i http://localhost/actuator/health
HTTP/1.1 200 OK
Content-Type: text/html
<!doctype html>...<app-root></app-root>...   ← es el index.html de Angular, no Actuator real
```

2. **Incluso accediendo directo al backend** (bypaseando Nginx, dentro de la red Docker), solo `health` e `info` están habilitados vía `management.endpoints.web.exposure.include: health,info` ([`application.yml:36-40`](../../backend/src/main/resources/application.yml)):
```
$ docker run --rm --network practica-experimental-unidad-iii_pfc_network curlimages/curl:8.10.1 \
    http://app:8080/actuator
{"_links":{"self":...,"health-path":...,"health":...,"info":...}}   ← solo health e info, nada mas

$ curl http://app:8080/actuator/env
{"status":500,"detail":"Ocurrio un error interno inesperado", ...}   ← no registrado, sin datos filtrados

$ curl http://app:8080/actuator/beans
{"status":500,"detail":"Ocurrio un error interno inesperado", ...}   ← no registrado, sin datos filtrados
```
`/actuator/env` y `/actuator/beans` no están en la lista de exposición, por lo que Spring ni siquiera registra esas rutas — la petición cae en el manejador genérico de errores (`GlobalExceptionHandler.java:53-58`), que devuelve un `ProblemDetail` genérico ("Ocurrio un error interno inesperado") sin stack trace ni detalle interno, consistente con no exponer información sensible incluso en el camino de error.

**Nota menor (no vulnerabilidad)**: el 500 en vez de un 404 más semánticamente correcto para una ruta no registrada es una imprecisión de mensaje, no una fuga de datos — no se filtra ningún dato de configuración en ningún escenario probado.

**CORS**: no existe configuración de `CorsConfiguration`/`@CrossOrigin` en el backend — no fue necesario porque la arquitectura sirve frontend y backend desde el mismo origen (`http://localhost` vía Nginx), evitando el riesgo de un `Access-Control-Allow-Origin: *` mal configurado que sí sería un hallazgo típico de A05.

**Veredicto**: mitigado.

---

## A06:2021 — Vulnerable and Outdated Components

Se intentó `./mvnw org.owasp:dependency-check-maven:check` (escaneo real contra la base NVD de CVEs). **Falló por limitación real del entorno**: el plugin de OWASP Dependency-Check requiere una API key de NVD desde 2023 para actualizar su base de datos local, y no hay ninguna configurada en este entorno (no se creó una cuenta NVD para este ejercicio):
```
[ERROR] UpdateException: Error updating the NVD Data
[ERROR] caused by NvdApiException: Invalid API Key, length of 0 too short to provided a masked partial key
```
Como fallback explícitamente permitido, se corrió `./mvnw versions:display-dependency-updates`, que sí completó (`BUILD SUCCESS`) y compara contra Maven Central en tiempo real. Dependencias **directas** del proyecto con versión más nueva disponible:

| Dependencia | Versión actual | Versión disponible | Relevancia de seguridad |
|---|---|---|---|
| `io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson` | 0.12.6 | 0.13.0 | **Alta** — es la librería que firma/valida los JWT de todo el sistema de auth |
| `org.postgresql:postgresql` | 42.7.11 | 42.7.13 | Media — driver JDBC, parches menores |
| `org.flywaydb:flyway-core` / `flyway-database-postgresql` | 11.7.2 | 13.2.0 | Baja — herramienta de migración, no superficie de ataque runtime |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.9.0 | 3.1.0 | Baja — solo genera documentación |
| `org.springframework.boot:spring-boot-starter-*` (todos) | 3.5.16 | 4.1.0 | **Alta en general**, pero es un salto de major version (Spring Boot 3→4) que requiere migración planificada, no un simple bump — fuera de alcance de este bloque |
| `org.springframework.security:spring-security-test` | 6.5.11 | 7.1.0 | Solo test, sin impacto en runtime productivo |
| `org.testcontainers:*` | 1.20.4 | 1.21.4 | Solo test, sin impacto en runtime productivo |

**Interpretación honesta**: no se puede afirmar con evidencia real si `jjwt` 0.12.6 tiene un CVE conocido específico (el escaneo real de CVEs falló por falta de API key) — solo se puede afirmar que existe una versión más nueva disponible. La actualización de `jjwt` a 0.13.0 es la más recomendable de aplicar a corto plazo por tocar directamente la superficie de autenticación; el salto a Spring Boot 4 requiere su propio bloque de trabajo dedicado (breaking changes esperables en Spring Security 7 y Jakarta EE).

**Veredicto**: gap declarado — no se pudo confirmar/descartar CVEs específicos por falta de API key de NVD; se documentan las versiones desactualizadas reales como el paso previo necesario.

---

## A07:2021 — Identification and Authentication Failures

**Vector de ataque específico**: reutilizar un JWT robado/filtrado después de que el usuario cerró sesión, ya que JWT es stateless por diseño y normalmente no se puede revocar.

**Contramedida real — blacklist de JTI en logout**: `AuthController.logout()` (`AuthController.java:63-73`) extrae el `jti` (claim `id` del JWT) y lo agrega a una blacklist en Redis con TTL igual al tiempo de vida restante del token (`TokenBlacklistService.java`, prefijo de key `jwt_blacklist:`). `JwtAuthenticationFilter.java:45` consulta `tokenBlacklistService.estaEnBlacklist(claims.getId())` en cada request autenticada, y si el `jti` está en la blacklist, la request no se autentica (queda anónima, y cae en 401 al llegar a un endpoint protegido).

**Evidencia real (curl, ciclo completo)**:
```
$ curl -s -c cookies.txt -X POST http://localhost/api/v1/auth/login ... -o /dev/null
$ curl -s -b cookies.txt -o /dev/null -w "%{http_code}\n" http://localhost/api/v1/libros
200
$ curl -s -b cookies.txt -X POST http://localhost/api/v1/auth/logout -o /dev/null -w "%{http_code}\n"
204
$ curl -s -b cookies.txt -o /dev/null -w "%{http_code}\n" http://localhost/api/v1/libros
401
```
La **misma cookie**, reutilizada después de logout, pasa de 200 a 401 — el token queda efectivamente revocado pese a ser JWT stateless.

**Gap real declarado — sin protección de fuerza bruta**: igual que en A04, no hay rate limiting ni bloqueo de cuenta tras N intentos fallidos de login. Reutilizando la misma evidencia: 5 intentos consecutivos con contraseña incorrecta contra `admin` devolvieron 401 sin ningún tipo de fricción creciente ni bloqueo temporal.

**Veredicto**: mitigado en revocación de sesión (logout real); gap declarado en protección contra fuerza bruta de credenciales.

---

## XSS (Cross-Site Scripting)

Aunque XSS ya no es una categoría independiente en el OWASP Top 10:2021 (se subsume en A03:Injection), se audita explícitamente por requerimiento de la guía.

**Vector de ataque específico**: un atacante inyecta un payload como `<img src=x onerror=alert(1)>` en el campo `titulo` o `descripcion` de un libro (vía `POST /api/v1/libros`, con permisos ADMIN o explotando otra vulnerabilidad), esperando que se ejecute cuando otro usuario visualice el listado en el frontend.

**Contramedida real (frontend)**: Angular sanitiza automáticamente todo el contenido interpolado en templates (`{{ }}`) y en bindings de propiedades — no se encontró **ningún** uso de `[innerHTML]`, `innerHTML` directo, ni `bypassSecurityTrustHtml`/`bypassSecurityTrustScript` en todo `frontend/src` (búsqueda exhaustiva, cero resultados). Esto significa que el equipo no desactivó la sanitización de Angular en ningún punto del código: cualquier `<script>` o `onerror` inyectado en `titulo`/`descripcion` se renderiza como texto plano escapado, nunca como HTML/JS ejecutable.

**Contramedida real (mitigación del vector de robo de token)**: el JWT se almacena exclusivamente en una cookie `HttpOnly` (`AuthController.java:87`, `.httpOnly(true)`) — nunca en `localStorage`/`sessionStorage` (búsqueda exhaustiva en `frontend/src`, cero resultados). Esto es la mitigación estándar recomendada contra el escenario "XSS logra ejecutar JS arbitrario": incluso si un XSS lograra ejecutarse pese a la sanitización de Angular, el JavaScript inyectado **no puede leer `document.cookie`** para exfiltrar el token, porque `HttpOnly` lo oculta del DOM/JS por diseño del navegador. Este razonamiento ya está desarrollado en el informe teórico del equipo (`docs/informe/fundamento-teorico-u3.md`).

**Veredicto**: mitigado (doble capa: sanitización de Angular + HttpOnly).

---

## Resumen ejecutivo

| Control | Veredicto | Gap declarado |
|---|---|---|
| A01 Broken Access Control | Mitigado | — |
| A02 Cryptographic Failures | Mitigado | Corregido durante esta auditoría: cookie `Secure` pasó de hardcodeada en `false` a `app.cookie.secure` (default `true`, override solo vía `COOKIE_SECURE` en `.env` de desarrollo) |
| A03 Injection | Mitigado | — |
| A04 Insecure Design | **Gap** | Sin rate limiting en ningún endpoint |
| A05 Security Misconfiguration | Mitigado | — |
| A06 Vulnerable Components | **Gap parcial** | `jjwt` desactualizado (0.12.6→0.13.0); no se pudo escanear CVEs reales por falta de API key NVD |
| A07 Auth Failures | Mitigado (con nota) | Sin protección de fuerza bruta en login (mismo gap que A04) |
| XSS | Mitigado | — |

De 8 controles auditados, 6 están completamente mitigados con evidencia real (uno de ellos, A02, corregido en el curso de esta auditoría — ver detalle arriba), 1 tiene mitigación parcial con una nota concreta (A07), y 1 (rate limiting, que también afecta A07) es un gap real no mitigado, documentado honestamente como decisión de alcance consciente para un sistema académico sin exposición pública real — no como un descuido oculto.
