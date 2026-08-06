workspace "PFC - Gestion Bibliotecaria" "Documentacion de arquitectura C4 (Nivel 1 y 2) del sistema de gestion bibliotecaria (ejercicio de practica - Unidad III, Aplicaciones Web)." {

    model {
        lector = person "Lector" "Consulta el catalogo de libros disponibles."
        administrador = person "Administrador" "Gestiona el catalogo de libros (crear, actualizar, eliminar). Requiere autenticacion."

        sistemaPfc = softwareSystem "Sistema PFC - Gestion Bibliotecaria" "Sistema de gestion bibliotecaria (ejercicio de practica - Unidad III, Aplicaciones Web). Permite consultar y administrar el catalogo de libros." {

            frontend = container "Frontend Angular" "Interfaz web (SPA) que permite consultar el catalogo y, si el usuario esta autenticado como administrador, gestionar libros." "Angular 17+" "Frontend"

            backend = container "Backend Spring Boot" "Expone la API REST del catalogo de libros y la autenticacion, aplicando las reglas de negocio (capas Controller-Service-Repository-Entity)." "Java 21 / Spring Boot 3.5.x" "Backend" {

                # Nivel 3 - Componentes (inventariados desde controller/, service/, security/ y exception/GlobalExceptionHandler.java)

                authController = component "AuthController" "Expone login y logout (JWT via cookie HttpOnly); valida credenciales contra la base de datos y coordina la generacion/revocacion de tokens. No pasa por la capa de Servicios." "Spring MVC REST Controller" "Controller"

                libroController = component "LibroController" "Expone el CRUD del catalogo de libros y el listado paginado con filtro opcional por titulo." "Spring MVC REST Controller" "Controller"

                editorialController = component "EditorialController" "Expone el CRUD de editoriales (tabla maestra)." "Spring MVC REST Controller" "Controller"

                idiomaController = component "IdiomaController" "Expone el CRUD de idiomas (tabla maestra)." "Spring MVC REST Controller" "Controller"

                estadoLibroController = component "EstadoLibroController" "Expone el CRUD de estados de libro (tabla maestra)." "Spring MVC REST Controller" "Controller"

                capaServicios = component "Capa de Servicios de Catalogo" "Aplica las reglas de negocio del catalogo (Libro, Editorial, Idioma, EstadoLibro): validaciones, cache-aside del listado de libros y mapeo a DTOs de respuesta." "Spring @Service (LibroServiceImpl, EditorialServiceImpl, IdiomaServiceImpl, EstadoLibroServiceImpl)" "Service"

                repositoriosJpa = component "Repositorios JPA" "Interfaces Spring Data JPA para libros, editoriales, idiomas, estados de libro y usuarios." "Spring Data JPA" "Repository"

                filtroJwt = component "Filtro de Autenticacion JWT" "Intercepta cada peticion, valida el token de la cookie y consulta la blacklist de JTI en Redis; genera y revoca tokens para AuthController." "Spring Security Filter + JJWT (JwtAuthenticationFilter, JwtService, TokenBlacklistService)" "Security"

                manejadorExcepciones = component "Manejador Global de Excepciones" "Traduce excepciones de negocio, de validacion, de credenciales invalidas y no controladas a respuestas ProblemDetail (RFC 7807), de forma transversal a todos los controllers." "Spring @RestControllerAdvice (GlobalExceptionHandler)" "Exception"
            }

            baseDatos = container "Base de datos PostgreSQL" "Almacena de forma persistente el catalogo de libros y los usuarios del sistema." "PostgreSQL 16" "Database"

            redis = container "Redis" "Cumple doble funcion: (1) cache-aside de las consultas de listado de libros para reducir la carga sobre PostgreSQL, y (2) blacklist de JTI de tokens JWT para permitir logout efectivo en un esquema de autenticacion stateless." "Redis 7" "Cache"
        }

        lector -> frontend "Consulta el catalogo de libros usando" "HTTPS"
        administrador -> frontend "Administra el catalogo de libros usando" "HTTPS"

        frontend -> backend "Realiza llamadas a la API REST (incluye login/logout)" "HTTPS/REST (JSON)"
        backend -> baseDatos "Lee y escribe libros y usuarios" "JDBC"
        backend -> redis "Lee y escribe entradas de cache y blacklist de JTI" "RESP (Redis protocol)"

        # Nivel 3 - Relaciones entre componentes del backend (mismo nivel de detalle que las relaciones de arriba, pero por componente)

        frontend -> authController "Login / Logout" "HTTPS/REST (JSON)"
        frontend -> libroController "Consulta y administra el catalogo de libros" "HTTPS/REST (JSON)"
        frontend -> editorialController "Administra editoriales" "HTTPS/REST (JSON)"
        frontend -> idiomaController "Administra idiomas" "HTTPS/REST (JSON)"
        frontend -> estadoLibroController "Administra estados de libro" "HTTPS/REST (JSON)"

        authController -> repositoriosJpa "Consulta las credenciales del usuario (UsuarioRepository)" "JPA"
        authController -> filtroJwt "Genera el token en login y lo revoca (blacklist) en logout" "Java"

        libroController -> capaServicios "Delega listado y CRUD de libros"
        editorialController -> capaServicios "Delega CRUD de editoriales"
        idiomaController -> capaServicios "Delega CRUD de idiomas"
        estadoLibroController -> capaServicios "Delega CRUD de estados de libro"

        capaServicios -> repositoriosJpa "Persiste y consulta libros, editoriales, idiomas y estados" "JPA"
        capaServicios -> redis "Cache-aside del listado de libros (@Cacheable / @CacheEvict)" "RESP (Redis protocol)"

        repositoriosJpa -> baseDatos "Ejecuta las consultas SQL generadas" "JDBC"

        filtroJwt -> redis "Consulta y escribe la blacklist de JTI revocados" "RESP (Redis protocol)"
        libroController -> filtroJwt "Requiere autenticacion previa via filtro (requestMatchers /api/** authenticated())" "Java (Servlet Filter)"
        editorialController -> filtroJwt "Requiere autenticacion previa via filtro" "Java (Servlet Filter)"
        idiomaController -> filtroJwt "Requiere autenticacion previa via filtro" "Java (Servlet Filter)"
        estadoLibroController -> filtroJwt "Requiere autenticacion previa via filtro" "Java (Servlet Filter)"

        authController -> manejadorExcepciones "Excepciones de credenciales invalidas se traducen a ProblemDetail (cross-cutting via @RestControllerAdvice)"
        libroController -> manejadorExcepciones "Excepciones de negocio/validacion se traducen a ProblemDetail (cross-cutting via @RestControllerAdvice)"
        editorialController -> manejadorExcepciones "Excepciones de negocio/validacion se traducen a ProblemDetail (cross-cutting via @RestControllerAdvice)"
        idiomaController -> manejadorExcepciones "Excepciones de negocio/validacion se traducen a ProblemDetail (cross-cutting via @RestControllerAdvice)"
        estadoLibroController -> manejadorExcepciones "Excepciones de negocio/validacion se traducen a ProblemDetail (cross-cutting via @RestControllerAdvice)"
    }

    views {
        systemContext sistemaPfc "Nivel1-Contexto" {
            include *
            autoLayout
            title "Nivel 1 - Contexto del Sistema PFC"
            description "Actores Lector y Administrador interactuando con el Sistema PFC de Gestion Bibliotecaria."
        }

        container sistemaPfc "Nivel2-Contenedores" {
            include *
            autoLayout
            title "Nivel 2 - Contenedores del Sistema PFC"
            description "Descomposicion del Sistema PFC en sus contenedores: Frontend Angular, Backend Spring Boot, PostgreSQL y Redis."
        }

        component backend "Nivel3-Componentes" {
            include *
            autoLayout
            title "Nivel 3 - Componentes del Backend Spring Boot"
            description "Descomposicion del Backend Spring Boot en sus componentes principales: Controllers (Auth, Libro, Editorial, Idioma, EstadoLibro), Capa de Servicios de Catalogo, Repositorios JPA, Filtro de Autenticacion JWT y Manejador Global de Excepciones."
        }

        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Frontend" {
                background #85bbf0
                color #000000
            }
            element "Backend" {
                background #438dd5
                color #ffffff
            }
            element "Database" {
                shape cylinder
                background #438dd5
                color #ffffff
            }
            element "Cache" {
                shape cylinder
                background #f5a623
                color #000000
            }
            element "Controller" {
                background #6fb1fc
                color #000000
            }
            element "Service" {
                background #438dd5
                color #ffffff
            }
            element "Repository" {
                background #2e6da4
                color #ffffff
            }
            element "Security" {
                background #c0392b
                color #ffffff
            }
            element "Exception" {
                background #7f8c8d
                color #ffffff
            }
        }
    }

}
