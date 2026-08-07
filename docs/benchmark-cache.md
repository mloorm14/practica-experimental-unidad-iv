# Benchmark de cache — GET /api/libros

## Metodología

Se midió el tiempo de respuesta de `GET /api/libros` en dos escenarios, con el backend y Redis corriendo localmente (Docker Compose):

- **Sin cache**: se vació la cache (`redis-cli FLUSHDB`) antes de cada una de las 10 repeticiones, forzando que cada petición golpee la base de datos.
- **Con cache**: se vació la cache una sola vez, se hizo una petición de "calentamiento" (no medida) para poblarla, y luego se midieron 10 repeticiones consecutivas dentro de la ventana de TTL (5 minutos), todas sirviéndose desde Redis.

El speedup se calcula como `S = Tsin / Tcon` (tiempo promedio sin cache dividido entre tiempo promedio con cache).

Script usado: `backend/scripts/benchmark-cache.ps1`.

## Resultados crudos

| Repetición | Sin cache (ms) | Con cache (ms) |
|---|---|---|
| 1 | 429.17 | 28.03 |
| 2 | 14.80 | 8.47 |
| 3 | 14.88 | 7.85 |
| 4 | 13.31 | 9.90 |
| 5 | 18.06 | 7.43 |
| 6 | 14.64 | 9.45 |
| 7 | 15.37 | 7.34 |
| 8 | 14.27 | 8.75 |
| 9 | 13.25 | 8.40 |
| 10 | 13.70 | 10.15 |

## Interpretación

La primera repetición de cada bloque muestra un pico (429.17 ms sin cache, 28.03 ms con cache) atribuible al arranque en frío de la JVM y del pool de conexiones de HikariCP hacia PostgreSQL, no al comportamiento del cache en sí. Por eso se reportan dos cálculos:

| Cálculo | Promedio sin cache | Promedio con cache | Speedup S |
|---|---|---|---|
| Con todos los datos (incluye arranque en frío) | 56.15 ms | 10.58 ms | **5.31x** |
| Excluyendo la 1ª repetición de cada bloque (sistema ya "caliente") | 14.70 ms | 8.64 ms | **1.70x** |

El primer número (5.31x) sobreestima el beneficio real del cache porque compara un caso frío contra uno ya calentado. El segundo número (1.70x) es más representativo del beneficio que aporta Redis en un sistema en régimen estable: una reducción de aproximadamente el 41% en el tiempo de respuesta al evitar la consulta a PostgreSQL.

## Análisis estadístico

Se calculan, para cada escenario y cada uno de los dos bloques de datos (con todos los datos, y excluyendo la 1ª repetición), la desviación estándar muestral y el intervalo de confianza (IC) del 95% para la media, usando la distribución t de Student (n=10 y n=9 son muestras pequeñas, por lo que no corresponde usar z).

**Fórmulas usadas:**

- Media: x̄ = Σxᵢ / n
- Desviación estándar muestral: s = √[ Σ(xᵢ − x̄)² / (n − 1) ]
- Error estándar de la media: SE = s / √n
- Intervalo de confianza 95%: x̄ ± t(0.025, gl) · SE, con gl = n − 1
- Percentil 95 (P95): interpolación lineal tipo Hyndman-Fan 7 (método por defecto de `numpy.percentile`/`pandas.quantile`), sobre los datos ordenados xₛₒᵣₜ. Con posición 0-indexada h = (95/100)·(n − 1): P95 = xₛₒᵣₜ[⌊h⌋] + (h − ⌊h⌋) · (xₛₒᵣₜ[⌈h⌉] − xₛₒᵣₜ[⌊h⌋])

### Bloque 1 — Con todos los datos (incluye arranque en frío), n = 10, gl = 9, t crítico = 2.262

| Escenario | x̄ (ms) | s (ms) | SE = s/√n (ms) | Margen = t·SE (ms) | IC 95% | P95 (ms) |
|---|---|---|---|---|---|---|
| Sin cache | 56.15 | 131.07 | 41.45 | 93.76 | (−37.61, 149.90) | 244.17 |
| Con cache | 10.58 | 6.21 | 1.96 | 4.44 | (6.14, 15.02) | 19.98 |

Cálculo del P95 (Bloque 1, n = 10): h = 0.95 · 9 = 8.55 → posiciones 0-indexadas 8 y 9 del arreglo ordenado, fracción 0.55.
- Sin cache ordenado: [13.25, 13.31, 13.70, 14.27, 14.64, 14.80, 14.88, 15.37, 18.06, 429.17] → P95 = 18.06 + 0.55·(429.17 − 18.06) = **244.17 ms**
- Con cache ordenado: [7.34, 7.43, 7.85, 8.40, 8.47, 8.75, 9.45, 9.90, 10.15, 28.03] → P95 = 10.15 + 0.55·(28.03 − 10.15) = **19.98 ms**

### Bloque 2 — Excluyendo la 1ª repetición de cada bloque, n = 9, gl = 8, t crítico = 2.306

| Escenario | x̄ (ms) | s (ms) | SE = s/√n (ms) | Margen = t·SE (ms) | IC 95% | P95 (ms) |
|---|---|---|---|---|---|---|
| Sin cache | 14.70 | 1.46 | 0.486 | 1.12 | (13.58, 15.82) | 16.98 |
| Con cache | 8.64 | 1.02 | 0.342 | 0.79 | (7.85, 9.43) | 10.05 |

Cálculo del P95 (Bloque 2, n = 9): h = 0.95 · 8 = 7.6 → posiciones 0-indexadas 7 y 8 del arreglo ordenado, fracción 0.6.
- Sin cache ordenado: [13.25, 13.31, 13.70, 14.27, 14.64, 14.80, 14.88, 15.37, 18.06] → P95 = 15.37 + 0.6·(18.06 − 15.37) = **16.98 ms**
- Con cache ordenado: [7.34, 7.43, 7.85, 8.40, 8.47, 8.75, 9.45, 9.90, 10.15] → P95 = 9.90 + 0.6·(10.15 − 9.90) = **10.05 ms**

**Interpretación del solapamiento de los IC:**

En el Bloque 1 (con el dato de arranque en frío incluido), el IC de "sin cache" (−37.61, 149.90) es tan ancho —por la enorme dispersión que introduce el outlier de 429.17 ms— que contiene por completo al IC de "con cache" (6.14, 15.02). Los intervalos se solapan, y con esta única evidencia no se puede afirmar que la diferencia observada sea estadísticamente significativa: podría deberse a ruido de muestreo. En el Bloque 2 (régimen estable, sin el outlier), el IC de "sin cache" (13.58, 15.82) y el de "con cache" (7.85, 9.43) **no se solapan** — el límite inferior de "sin cache" (13.58) queda muy por encima del límite superior de "con cache" (9.43). Esto sí sustenta que la diferencia entre ambos escenarios es estadísticamente significativa al 95% en condiciones de régimen estable, y no un artefacto del muestreo.

**IC del speedup en régimen estable (Bloque 2):**

Para el cociente S = x̄_sin / x̄_con de dos medias independientes se aproxima su error estándar por el método delta (propagación de errores relativos):

SE(S) ≈ S · √[ (SE_sin / x̄_sin)² + (SE_con / x̄_con)² ]

Sustituyendo: SE(S) ≈ 1.70 · √[ (0.486/14.70)² + (0.342/8.64)² ] = 1.70 · √[ 0.03306² + 0.03958² ] = 1.70 · √0.002657 ≈ 1.70 · 0.0515 ≈ 0.088

Margen = t(0.025, 8) · SE(S) = 2.306 · 0.088 ≈ 0.20

IC 95% del speedup en régimen estable: **1.70 ± 0.20 → (1.50, 1.90)**

## Conclusión

El cache-aside implementado con `@Cacheable`/`@CacheEvict` sobre `GET /api/libros` reduce el tiempo de respuesta de forma consistente. El beneficio medido en condiciones estables (sin efectos de arranque en frío) es de aproximadamente **1.7x** (IC 95%: 1.50x–1.90x), cifra que crecería en escenarios con consultas más costosas (más registros, joins, filtros complejos) donde el costo de ir a base de datos pesa más frente al costo fijo de leer de Redis.