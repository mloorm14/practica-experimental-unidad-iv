# Prueba de carga — Apache Bench (ab)

## Entorno y herramienta

`ab` no está disponible nativamente en el host de desarrollo (Windows, sin `apt`/`apache2-utils`). En vez de instalarlo a nivel de sistema con Chocolatey (persistente e innecesario para una sola sesión de pruebas), se ejecutó desde un contenedor Docker descartable (`alpine:3.20` + `apk add apache2-utils`) conectado a la misma red de Docker Compose (`practica-experimental-unidad-iii_pfc_network`), apuntando al servicio `nginx` por su nombre DNS interno (`http://nginx/...`, puerto 80). Esto ejercita exactamente el mismo Nginx que escucha en el puerto 80 publicado al host — es el mismo reverse proxy, solo alcanzado desde dentro de la red Docker en vez de a través del port-mapping, sin instalar nada en el sistema operativo anfitrión.

```
docker run -d --name ab-runner --network practica-experimental-unidad-iii_pfc_network alpine:3.20 sleep 3600
docker exec ab-runner apk add --no-cache apache2-utils
```

## Metodología

- **Endpoint probado**: `GET /api/v1/libros` — el listado paginado de libros, el endpoint más usado del sistema y el que tiene cache-aside implementado (`LibroServiceImpl.listar`, `@Cacheable(value = CACHE_LISTADO, key = "#titulo + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")`). Al llamarse siempre sin query params, todas las requests de una corrida caen en la misma key de cache.
- **Ruta de red**: contra Nginx (puerto 80, reverse proxy real) — no directo al backend. `nginx/nginx.conf` reenvía `/api/` a `http://app:8080/api/`.
- **Autenticación**: token JWT real obtenido de `POST /api/v1/auth/login` con el usuario admin sembrado por Flyway (`V2__crear_tabla_usuarios.sql`), extraído de la cookie `access_token` (HttpOnly) y reenviado como header `Authorization: Bearer <token>` en `ab -H`, ejercitando la extracción dual del filtro (`JwtAuthenticationFilter`).
- **Concurrencia y volumen**: `c=20` usuarios concurrentes, `n=500` requests totales — carga razonable para un PFC académico, sin saturar Docker Desktop local.
- **Dos corridas**, para conectar con el patrón de cache-aside ya documentado en [ADR-003](../adr/ADR-003-redis-cache-y-blacklist-jwt.md) y [benchmark-cache.md](../benchmark-cache.md):
  1. **Cache frío**: `redis-cli FLUSHALL` inmediatamente antes de la corrida (verificado `DBSIZE` = 0 tras el flush).
  2. **Cache caliente**: segunda corrida inmediatamente después de la primera, sin tocar Redis — se beneficia de la cache ya poblada por la corrida anterior.

## Comando `ab` exacto usado (idéntico en ambas corridas, solo cambia el estado de la cache)

```
docker exec ab-runner ab -n 500 -c 20 -H "Authorization: Bearer <token>" http://nginx/api/v1/libros
```

## Preparación del cache frío

```
$ docker compose exec redis redis-cli FLUSHALL
OK
$ docker compose exec redis redis-cli DBSIZE
0
```

## Resultado crudo — Corrida 1: cache frío

```
This is ApacheBench, Version 2.3 <$Revision: 1923142 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking nginx (be patient)
Completed 100 requests
Completed 200 requests
Completed 300 requests
Completed 400 requests
Completed 500 requests
Finished 500 requests


Server Software:        nginx/1.31.3
Server Hostname:        nginx
Server Port:            80

Document Path:          /api/v1/libros
Document Length:        9524 bytes

Concurrency Level:      20
Time taken for tests:   0.623 seconds
Complete requests:      500
Failed requests:        0
Total transferred:      4910500 bytes
HTML transferred:       4762000 bytes
Requests per second:    802.66 [#/sec] (mean)
Time per request:       24.917 [ms] (mean)
Time per request:       1.246 [ms] (mean, across all concurrent requests)
Transfer rate:          7698.19 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.5      0       3
Processing:    10   23   5.0     23      37
Waiting:        9   21   4.8     21      35
Total:         10   23   5.1     23      37

Percentage of the requests served within a certain time (ms)
  50%     23
  66%     25
  75%     26
  80%     27
  90%     30
  95%     32
  98%     34
  99%     36
 100%     37 (longest request)
```

## Resultado crudo — Corrida 2: cache caliente (inmediatamente después, sin flush)

```
This is ApacheBench, Version 2.3 <$Revision: 1923142 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking nginx (be patient)
Completed 100 requests
Completed 200 requests
Completed 300 requests
Completed 400 requests
Completed 500 requests
Finished 500 requests


Server Software:        nginx/1.31.3
Server Hostname:        nginx
Server Port:            80

Document Path:          /api/v1/libros
Document Length:        9524 bytes

Concurrency Level:      20
Time taken for tests:   0.507 seconds
Complete requests:      500
Failed requests:        0
Total transferred:      4910500 bytes
HTML transferred:       4762000 bytes
Requests per second:    986.80 [#/sec] (mean)
Time per request:       20.268 [ms] (mean)
Time per request:       1.013 [ms] (mean, across all concurrent requests)
Transfer rate:          9464.21 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.6      0       5
Processing:     8   19   4.3     18      34
Waiting:        8   18   4.2     17      34
Total:          9   19   4.3     19      34
WARNING: The median and mean for the initial connection time are not within a normal deviation
        These results are probably not that reliable.

Percentage of the requests served within a certain time (ms)
  50%     19
  66%     21
  75%     22
  80%     23
  90%     26
  95%     27
  98%     29
  99%     30
 100%     34 (longest request)
```

La advertencia de `ab` sobre el tiempo de conexión inicial es una observación estándar de la propia herramienta cuando la desviación estándar del *connect time* es proporcionalmente alta frente a una media muy pequeña (0–5 ms) — es decir, ruido normal de red/loopback en un ambiente local, no un problema del sistema bajo prueba. No se alteró ni se omitió del output.

## Resumen comparativo (extraído directamente del output de arriba, sin recalcular)

| Métrica | Cache frío | Cache caliente | Diferencia |
|---|---|---|---|
| Requests completados | 500 | 500 | — |
| Requests fallidos | 0 | 0 | 0% de error en ambas |
| Requests/seg (throughput) | 802.66 | 986.80 | +22.9% |
| Tiempo medio por request | 24.917 ms | 20.268 ms | −18.7% |
| P50 (mediana) | 23 ms | 19 ms | −17.4% |
| P95 | 32 ms | 27 ms | −15.6% |
| P99 | 36 ms | 30 ms | −16.7% |
| Máximo | 37 ms | 34 ms | −8.1% |

## Interpretación

Ambas corridas terminaron con **0 requests fallidos** — el sistema respondió correctamente a las 1000 requests totales bajo 20 usuarios concurrentes. La corrida con cache caliente fue consistentemente más rápida en todos los percentiles (P50 a P99) y sostuvo mayor throughput, confirmando en carga concurrente el mismo patrón de mejora que [benchmark-cache.md](../benchmark-cache.md) ya había medido en solicitudes secuenciales individuales (speedup ~1.7x en régimen estable). La magnitud del speedup es menor aquí (~1.23x vs ~1.7x) porque bajo `ab` el cuello de botella se reparte entre Nginx, la serialización JSON y la red del contenedor, no solo la consulta a PostgreSQL — con 20 requests concurrentes golpeando la misma key de cache, la mayoría ya se benefician de Redis incluso en la "corrida fría" después de las primeras iteraciones, atenuando el contraste frente al escenario secuencial.

## Limpieza

```
$ docker rm -f ab-runner
```
