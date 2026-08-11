# Reflexión individual — Escudero Plaza María del Rosario (tests de integración)

> Borrador para la sección 5.4 del informe técnico final de la Unidad IV. Basado en hechos reales de este repositorio, para que María lo revise, corrija y ajuste como reflexión personal propia antes de incorporarlo al documento final. No es una versión final ni una redacción ajena a la que ella deba someterse tal cual.

Lo más valioso de la Unidad IV para mí fue que los tests de integración me obligaron a leer el código real en lugar de asumir cómo funcionaba. Llegué con tres supuestos que los tests se encargaron de derribar, y cada uno dejó una lección distinta.

El primero fue ambiental y casi no tenía que ver con la API. El proyecto declara Java 21, pero mi máquina tenía instalado JDK 25, y los tests no compilaban: Lombok no generaba los métodos que el código esperaba. Resulta que desde JDK 23 los procesadores de anotación ya no se ejecutan por defecto, así que tuve que correr el build con `-Dmaven.compiler.proc=full` antes de cada verificación (commits `2cfe9fd`, `6f2d57a` y `cbbff6f`). La lección es que la cadena de build es parte del entorno de trabajo: un "no compila" en mi máquina no significaba que el proyecto estuviera roto, sino que mi entorno no replicaba el del proyecto.

El segundo fue de lectura de contrato. Mi primer test de logout esperaba un 200 con cuerpo, y el endpoint devuelve 204 No Content. Fue la API diciéndome cómo es, no cómo yo imaginaba que debía ser. Lo mismo pasó con la creación de libros: mi fixture tenía solo titulo, autor e isbn, y el DTO `LibroRequest` exige además `anioPublicacion`, `editorialId`, `idiomaId`, `estadoId` y `stock`, por eso el "POST con rol ADMIN" devolvía 400 en vez de 201. Aprendí que antes de construir un fixture hay que leer el DTO y el controlador, y que un test que falla por el contrato es un test que está haciendo su trabajo.

El tercero fue el más técnico. Al separar los tests en dos clases compartí los contenedores de Testcontainers como campos estáticos de una clase base; la primera clase terminaba y detenía los contenedores, y la segunda ya no podía reconectarse — todo devolvía 500. La solución fue darle a cada clase sus propios contenedores. Ahí entendí que el aislamiento entre tests también aplica a la infraestructura, no solo a los datos.

Si empezara de nuevo, leería primero el contrato completo de cada endpoint (DTO, validaciones y códigos de estado reales) antes de escribir el primer test, y tendría presente que verificar en vivo el comportamiento real es parte del test, no un paso aparte.
