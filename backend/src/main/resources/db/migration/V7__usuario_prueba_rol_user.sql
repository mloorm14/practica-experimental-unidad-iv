-- El seed original (V2) solo crea un usuario con rol ADMIN ('admin'). Para poder
-- probar la matriz de autorizacion @PreAuthorize (GET abierto a ADMIN/USER, POST/PUT/
-- DELETE exclusivo de ADMIN) hace falta al menos un usuario con rol USER.
--
-- Credenciales de prueba (solo entorno de desarrollo, NO son datos de produccion):
--   username: usuario_prueba
--   password: User123!
--
-- Hash generado con BCryptPasswordEncoder() (costo 10 por defecto de Spring Security,
-- mismo formato/costo que el hash de 'admin' insertado en V2).
INSERT INTO usuarios (username, password, rol)
VALUES ('usuario_prueba', '$2a$10$g.sfnPq064MdYdy51QFm.eyliI/nVQZOIRexvKQZMv/fQUCsarLqO', 'USER');
