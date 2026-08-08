> **DOCUMENTO HISTORICO (Unidad III).** Se conserva como referencia del trabajo ya entregado y calificado. Para el estado actual del proyecto ver los documentos equivalentes de la Unidad IV en esta misma carpeta.

# Análisis de incoherencias: Guía de Práctica vs. Enunciado de Tarea

Este documento registra las diferencias detectadas entre la guía de práctica general del curso y el enunciado puntual de esta tarea, y cómo se resolvió cada una. Sirve como referencia si algo en el código o en la documentación no parece coincidir con lo que dice la guía marco del curso: la resolución ya fue tomada y está justificada aquí.

## 1. Stack tecnológico

La guía ofrece 3 opciones (Laravel/PHP, ASP.NET Core, Spring Boot). El enunciado fija Spring Boot + Angular + PostgreSQL como la opción concreta dentro de ese marco. No es contradicción, es particularización: Spring Boot + Hibernate/JPA ya estaba contemplado como opción válida en la guía.

## 2. Base de datos

Guía pide MySQL 8.0 (Docker). Enunciado pide PostgreSQL 16. Diferencia real. Resuelto usando PostgreSQL como único motor (manda el enunciado, más específico para esta entrega).

## 3. Gestión de sesión/JWT

La guía pedía comparar mecanismos de estado (cookies/localStorage/sessionStorage/JWT) como fundamento teórico, dejando abierto el mecanismo a implementar. El enunciado decide: JWT en cookie HttpOnly + blacklist de JTI en Redis. Coherente: el enunciado es la decisión concreta de lo que la guía pedía analizar en abstracto.

## 4. Frontend

La guía no manda frontend específico (se enfoca en el framework backend). El enunciado exige Angular 17+. El ADR-002 documenta y justifica esta decisión, incluyendo la comparación con React que exige el enunciado.

## 5. Diagramas C4

Guía pide niveles 1, 2 y 3. Enunciado pide solo niveles 1 y 2. Resuelto: se cumple el enunciado (niveles 1 y 2) como mínimo obligatorio de esta entrega puntual. El nivel 3 queda como extensión opcional si el equipo tiene tiempo, para cubrir también el estándar de la guía marco.

## 6. Cantidad de ADR

Guía pide mínimo 3 (arquitectura, ORM, caché). Enunciado pide mínimo 2 específicos (Redis/JWT, Angular vs React). Resuelto: se hicieron 3 ADR (arquitectura en capas, Angular vs React, Redis cache+blacklist JWT), cubriendo ambos requisitos sin contradicción.

## 7. Norma de citas bibliográficas

Guía pide referencias mínimas en formato IEEE para el fundamento teórico. Enunciado pide APA con mínimo 5 fuentes académicas. Choque real de formato. Resuelto: el informe usa APA en su totalidad (manda el enunciado, instrucción más específica de esta tarea), conservando las referencias IEEE de la guía como bibliografía complementaria de la sección de Fundamento Teórico, con aclaración explícita en el informe de qué norma se usa y por qué.

## Cambio de dominio: de Producto a Libro

Tras iniciar el proyecto con un dominio genérico de "Producto/inventario", se detectó que el enunciado real de la materia esperaba alinear el ejercicio con el contexto de gestión bibliotecaria trabajado en la Unidad III de Aplicaciones Web. Se migró el dominio completo — entidad, DTOs, repository, service, controller, nombre de la cache Redis, diagramas C4 y ADR — manteniendo la disciplina de versionado: el esquema se ajustó con una migración Flyway nueva (`V3__renombrar_producto_a_libro.sql`, vía `ALTER TABLE`, sin recrear la tabla ni perder datos existentes) en vez de editar las migraciones ya aplicadas, y todo el cambio de código quedó en un commit de `refactor` separado, sin reescribir ni perder el historial de commits previo.
