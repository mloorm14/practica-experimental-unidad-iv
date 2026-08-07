-- Introduce la entidad Autor con relacion N:M real hacia libros (un libro puede tener
-- varios autores, un autor puede tener varios libros), reemplazando gradualmente el
-- campo libros.autor (texto libre suelto). La columna libros.autor NO se elimina en
-- esta migracion: se mantiene en paralelo hasta confirmar explicitamente su borrado.

-- ============================================================
-- 1. Tabla autores
-- ============================================================
CREATE TABLE autores (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(150) NOT NULL,
    nacionalidad  VARCHAR(100),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_autores_nombre UNIQUE (nombre)
);

-- ============================================================
-- 2. Tabla intermedia libro_autor (N:M)
-- ============================================================
CREATE TABLE libro_autor (
    libro_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    CONSTRAINT pk_libro_autor PRIMARY KEY (libro_id, autor_id),
    CONSTRAINT fk_libro_autor_libro FOREIGN KEY (libro_id) REFERENCES libros (id),
    CONSTRAINT fk_libro_autor_autor FOREIGN KEY (autor_id) REFERENCES autores (id)
);

CREATE INDEX idx_libro_autor_autor_id ON libro_autor (autor_id);

-- ============================================================
-- 3. Migracion de datos: libros.autor (texto libre) -> autores + libro_autor
-- ============================================================

-- Crear un Autor por cada nombre distinto ya presente en libros.autor
INSERT INTO autores (nombre)
SELECT DISTINCT autor FROM libros WHERE autor IS NOT NULL AND autor != ''
ON CONFLICT (nombre) DO NOTHING;

-- Relacionar cada libro con el Autor correspondiente por nombre (case-insensitive,
-- igual que el criterio usado en V5 para editorial/idioma/estado)
INSERT INTO libro_autor (libro_id, autor_id)
SELECT l.id, a.id
FROM libros l
JOIN autores a ON LOWER(l.autor) = LOWER(a.nombre)
WHERE l.autor IS NOT NULL AND l.autor != '';
