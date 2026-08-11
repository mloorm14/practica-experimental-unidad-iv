# Reflexión individual — Escudero Plaza María del Rosario (tests de integración)

En esta unidad mi tarea fue escribir los tests de integración del backend. La guía pedía mínimo 10 tests y dejé 12, repartidos en dos clases: una para autenticación (login, logout y acceso sin token) y otra para el CRUD de libros y los permisos por rol.

Lo que más me costó no fue escribir los tests, sino hacer que funcionaran. Mi computadora tiene Java 25 instalado y el proyecto usa Java 21, así que al principio los tests ni siquiera compilaban: Lombok no generaba el código y tuve que correr el build con una bandera especial (`-Dmaven.compiler.proc=full`). Ahí entendí que el problema no era el código, sino que mi entorno no coincidía con el del proyecto.

También asumí cosas que no eran. Por ejemplo, pensé que el logout devolvía un 200 con un cuerpo, pero en realidad devuelve 204 (sin contenido). Y al probar la creación de libros, el test fallaba porque el formulario necesita más campos de los que yo le mandaba (año de publicación, editorial, idioma, estado y stock). Tuve que leer bien el DTO y el controlador para arreglarlo. Eso me enseñó a revisar el código antes de asumir cómo funciona un endpoint.

Otro problema fue con los contenedores de prueba. Al separar los tests en dos clases, compartí los mismos contenedores de base de datos y Redis, y cuando la primera clase terminaba, la segunda ya no podía conectarse y todo devolvía 500. La solución fue darle a cada clase sus propios contenedores. Me quedó claro que los tests tienen que estar aislados entre sí.

Al final el proyecto completo pasa con 48 tests y cero fallos. Si tuviera que repetir la unidad, empezaría leyendo el código de cada endpoint antes de escribir el primer test.
