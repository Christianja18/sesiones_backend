-- Script para crear la base de datos del sistema de sesiones academicas
-- Basado en el modelo de @file:problematica.md
-- Ejecutar en MySQL 8.0+
-- Incluye tablas, indices, vistas y datos de prueba
-- Revision de indices:
-- 1. Se mantienen PK, UNIQUE e indices utiles para consulta.
-- 2. Se eliminan indices redundantes cubiertos por claves UNIQUE compuestas.
-- 3. Se agregan indices practicos para consultas por docente y fecha.

CREATE DATABASE IF NOT EXISTS sesion_academica CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sesion_academica;

-- Nucleo del dominio: Curriculo
CREATE TABLE nivel_educativo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE COMMENT 'Ej: Inicial, Primaria, Secundaria'
) ENGINE=InnoDB;

CREATE TABLE ciclo (
    id VARCHAR(10) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE COMMENT 'Ej: III, IV, V, VI, VII'
) ENGINE=InnoDB;

CREATE TABLE grado (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nivel_id INT NOT NULL,
    ciclo_id VARCHAR(10) NOT NULL,
    nombre VARCHAR(50) NOT NULL COMMENT 'Ej: 1ro, 2do',
    FOREIGN KEY (nivel_id) REFERENCES nivel_educativo(id) ON DELETE CASCADE,
    FOREIGN KEY (ciclo_id) REFERENCES ciclo(id) ON DELETE RESTRICT,
    UNIQUE KEY uk_grado_nivel_nombre (nivel_id, nombre)
) ENGINE=InnoDB;

CREATE TABLE area (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE COMMENT 'Ej: Matematica, Comunicacion'
) ENGINE=InnoDB;

CREATE TABLE competencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    area_id INT NOT NULL,
    descripcion TEXT NOT NULL COMMENT 'Descripcion de la competencia',
    descripcion_hash CHAR(64) GENERATED ALWAYS AS (SHA2(REGEXP_REPLACE(TRIM(descripcion), '[[:space:]]+', ' '), 256)) STORED COMMENT 'Hash estable para unicidad de textos largos',
    FOREIGN KEY (area_id) REFERENCES area(id) ON DELETE CASCADE,
    UNIQUE KEY uk_competencia_area_hash (area_id, descripcion_hash)
) ENGINE=InnoDB;

CREATE TABLE capacidad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    competencia_id INT NOT NULL,
    descripcion TEXT NOT NULL COMMENT 'Descripcion de la capacidad',
    descripcion_hash CHAR(64) GENERATED ALWAYS AS (SHA2(REGEXP_REPLACE(TRIM(descripcion), '[[:space:]]+', ' '), 256)) STORED COMMENT 'Hash estable para unicidad de textos largos',
    FOREIGN KEY (competencia_id) REFERENCES competencia(id) ON DELETE CASCADE,
    UNIQUE KEY uk_capacidad_competencia_hash (competencia_id, descripcion_hash)
) ENGINE=InnoDB;

CREATE TABLE desempeno (
    id INT AUTO_INCREMENT PRIMARY KEY,
    grado_id INT NOT NULL,
    competencia_id INT NOT NULL,
    descripcion TEXT NOT NULL COMMENT 'Descripcion del desempeno',
    descripcion_hash CHAR(64) GENERATED ALWAYS AS (SHA2(REGEXP_REPLACE(TRIM(descripcion), '[[:space:]]+', ' '), 256)) STORED COMMENT 'Hash estable para unicidad de textos largos',
    fuente ENUM('oficial', 'ia') NOT NULL DEFAULT 'oficial' COMMENT 'Origen del desempeno',
    FOREIGN KEY (grado_id) REFERENCES grado(id) ON DELETE CASCADE,
    FOREIGN KEY (competencia_id) REFERENCES competencia(id) ON DELETE CASCADE,
    UNIQUE KEY uk_desempeno_grado_competencia_hash (grado_id, competencia_id, descripcion_hash)
) ENGINE=InnoDB;

CREATE TABLE estandar_aprendizaje (
    id INT AUTO_INCREMENT PRIMARY KEY,
    competencia_id INT NOT NULL,
    ciclo_id VARCHAR(10) NOT NULL,
    descripcion TEXT NOT NULL COMMENT 'Estandar de aprendizaje esperado por ciclo',
    descripcion_hash CHAR(64) GENERATED ALWAYS AS (SHA2(REGEXP_REPLACE(TRIM(descripcion), '[[:space:]]+', ' '), 256)) STORED COMMENT 'Hash estable para unicidad de textos largos',
    FOREIGN KEY (competencia_id) REFERENCES competencia(id) ON DELETE CASCADE,
    FOREIGN KEY (ciclo_id) REFERENCES ciclo(id) ON DELETE RESTRICT,
    UNIQUE KEY uk_estandar_competencia_ciclo_hash (competencia_id, ciclo_id, descripcion_hash),
    INDEX idx_estandar_competencia_ciclo (competencia_id, ciclo_id)
) ENGINE=InnoDB;

CREATE TABLE desempeno_capacidad (
    desempeno_id INT NOT NULL,
    capacidad_id INT NOT NULL,
    PRIMARY KEY (desempeno_id, capacidad_id),
    FOREIGN KEY (desempeno_id) REFERENCES desempeno(id) ON DELETE CASCADE,
    FOREIGN KEY (capacidad_id) REFERENCES capacidad(id) ON DELETE CASCADE,
    INDEX idx_desempeno_capacidad_capacidad (capacidad_id)
) ENGINE=InnoDB;

CREATE TABLE estandar_desempeno (
    estandar_id INT NOT NULL,
    desempeno_id INT NOT NULL,
    PRIMARY KEY (estandar_id, desempeno_id),
    FOREIGN KEY (estandar_id) REFERENCES estandar_aprendizaje(id) ON DELETE CASCADE,
    FOREIGN KEY (desempeno_id) REFERENCES desempeno(id) ON DELETE CASCADE,
    INDEX idx_estandar_desempeno_desempeno (desempeno_id)
) ENGINE=InnoDB;

-- Acceso del sistema: docentes/profesores
CREATE TABLE institucion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    UNIQUE KEY uk_institucion_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE rol (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL COMMENT 'ADMIN, PROFESOR',
    descripcion VARCHAR(255) NULL,
    UNIQUE KEY uk_rol_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE docente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    institucion_id INT NULL,
    rol_id INT NULL,
    password_hash VARCHAR(255) NULL COMMENT 'Hash compatible con Spring Security PasswordEncoder',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (institucion_id) REFERENCES institucion(id) ON DELETE RESTRICT,
    FOREIGN KEY (rol_id) REFERENCES rol(id) ON DELETE RESTRICT,
    INDEX idx_docente_institucion (institucion_id),
    INDEX idx_docente_rol_activo (rol_id, activo)
) ENGINE=InnoDB;

-- Planificacion
CREATE TABLE unidad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    grado_id INT NOT NULL,
    area_id INT NOT NULL,
    docente_id INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    contexto TEXT COMMENT 'Contexto del aula',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (grado_id) REFERENCES grado(id) ON DELETE CASCADE,
    FOREIGN KEY (area_id) REFERENCES area(id) ON DELETE CASCADE,
    FOREIGN KEY (docente_id) REFERENCES docente(id) ON DELETE CASCADE,
    CONSTRAINT chk_unidad_fechas CHECK (fecha_fin >= fecha_inicio),
    INDEX idx_grado_area_docente (grado_id, area_id, docente_id),
    INDEX idx_unidad_docente_fecha (docente_id, fecha_inicio, fecha_fin)
) ENGINE=InnoDB;

CREATE TABLE sesion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    unidad_id INT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    proposito TEXT NOT NULL,
    duracion_minutos INT NOT NULL,
    fecha DATE,
    generado_por_ia BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (unidad_id) REFERENCES unidad(id) ON DELETE CASCADE,
    CONSTRAINT chk_sesion_duracion CHECK (duracion_minutos > 0),
    INDEX idx_unidad_id (unidad_id),
    INDEX idx_sesion_fecha (fecha)
) ENGINE=InnoDB;

-- Contenido pedagogico
CREATE TABLE sesion_competencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    competencia_id INT NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    FOREIGN KEY (competencia_id) REFERENCES competencia(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sesion_competencia (sesion_id, competencia_id)
) ENGINE=InnoDB;

CREATE TABLE sesion_capacidad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    capacidad_id INT NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    FOREIGN KEY (capacidad_id) REFERENCES capacidad(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sesion_capacidad (sesion_id, capacidad_id)
) ENGINE=InnoDB;

CREATE TABLE sesion_desempeno (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    desempeno_id INT NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    FOREIGN KEY (desempeno_id) REFERENCES desempeno(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sesion_desempeno (sesion_id, desempeno_id)
) ENGINE=InnoDB;

-- Evaluacion
CREATE TABLE criterio_evaluacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    descripcion TEXT NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    INDEX idx_criterio_sesion (sesion_id)
) ENGINE=InnoDB;

CREATE TABLE evidencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    descripcion TEXT NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    INDEX idx_evidencia_sesion (sesion_id)
) ENGINE=InnoDB;

CREATE TABLE instrumento_evaluacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    tipo ENUM('rubrica', 'lista_cotejo') NOT NULL,
    contenido_json JSON NOT NULL COMMENT 'Detalles en formato JSON',
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    INDEX idx_instrumento_sesion (sesion_id)
) ENGINE=InnoDB;

-- Secuencia didactica
CREATE TABLE actividad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sesion_id INT NOT NULL,
    tipo ENUM('inicio', 'desarrollo', 'cierre') NOT NULL,
    descripcion TEXT NOT NULL,
    orden INT NOT NULL COMMENT 'Orden de la actividad',
    FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE,
    CONSTRAINT chk_actividad_orden CHECK (orden > 0),
    INDEX idx_sesion_tipo (sesion_id, tipo),
    UNIQUE KEY uk_sesion_orden (sesion_id, orden)
) ENGINE=InnoDB;

-- Registro de documentos curriculares como fuente bruta
CREATE TABLE documento_curriculo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('curriculo', 'programa') NOT NULL COMMENT 'curriculo: competencias/capacidades; programa: desempenos por grado',
    nombre_archivo VARCHAR(255) NOT NULL,
    archivo_url VARCHAR(500) NOT NULL,
    checksum_sha256 CHAR(64) NULL,
    estado ENUM('PENDIENTE', 'PROCESANDO', 'PROCESADO', 'ERROR') NOT NULL DEFAULT 'PENDIENTE',
    error_detalle TEXT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_procesado TIMESTAMP NULL,
    UNIQUE KEY uk_documento_curriculo_url (archivo_url),
    UNIQUE KEY uk_documento_curriculo_checksum (checksum_sha256),
    INDEX idx_documento_curriculo_estado (estado),
    INDEX idx_documento_curriculo_tipo_estado (tipo, estado)
) ENGINE=InnoDB;

CREATE TABLE documento_chunk (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento_id INT NOT NULL,
    orden INT NOT NULL,
    pagina_inicio INT NULL,
    pagina_fin INT NULL,
    contenido TEXT NOT NULL,
    hash_contenido CHAR(64) NOT NULL,
    estado ENUM('PENDIENTE', 'PROCESANDO', 'PROCESADO', 'ERROR') NOT NULL DEFAULT 'PENDIENTE',
    FOREIGN KEY (documento_id) REFERENCES documento_curriculo(id) ON DELETE CASCADE,
    UNIQUE KEY uk_documento_chunk_orden (documento_id, orden),
    UNIQUE KEY uk_documento_chunk_hash (documento_id, hash_contenido),
    INDEX idx_documento_chunk_estado (estado)
) ENGINE=InnoDB;

CREATE TABLE ingest_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento_id INT NULL,
    chunk_id INT NULL,
    estado ENUM('OK', 'ERROR') NOT NULL,
    mensaje TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (documento_id) REFERENCES documento_curriculo(id) ON DELETE SET NULL,
    FOREIGN KEY (chunk_id) REFERENCES documento_chunk(id) ON DELETE SET NULL,
    INDEX idx_ingest_log_documento (documento_id),
    INDEX idx_ingest_log_chunk (chunk_id),
    INDEX idx_ingest_log_estado (estado)
) ENGINE=InnoDB;

CREATE TABLE documento_chunk_clasificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chunk_id INT NOT NULL,
    area_id INT NULL,
    grado_id INT NULL,
    nivel_id INT NULL,
    ciclo_id VARCHAR(10) NULL,
    confianza DECIMAL(5,2) NULL,
    modelo VARCHAR(100) NULL,
    FOREIGN KEY (chunk_id) REFERENCES documento_chunk(id) ON DELETE CASCADE,
    FOREIGN KEY (area_id) REFERENCES area(id) ON DELETE SET NULL,
    FOREIGN KEY (grado_id) REFERENCES grado(id) ON DELETE SET NULL,
    FOREIGN KEY (nivel_id) REFERENCES nivel_educativo(id) ON DELETE SET NULL,
    FOREIGN KEY (ciclo_id) REFERENCES ciclo(id) ON DELETE SET NULL,
    INDEX idx_chunk_clasificacion_chunk (chunk_id),
    INDEX idx_chunk_clasificacion_area_grado (area_id, grado_id),
    INDEX idx_chunk_clasificacion_nivel_ciclo (nivel_id, ciclo_id)
) ENGINE=InnoDB;

-- Vistas utiles para consultas comunes
CREATE VIEW vista_sesion_competencias AS
SELECT
    s.id AS sesion_id,
    s.titulo AS sesion_titulo,
    s.proposito,
    s.duracion_minutos,
    s.generado_por_ia,
    GROUP_CONCAT(c.descripcion SEPARATOR '; ') AS competencias
FROM sesion s
LEFT JOIN sesion_competencia sc ON s.id = sc.sesion_id
LEFT JOIN competencia c ON sc.competencia_id = c.id
GROUP BY s.id, s.titulo, s.proposito, s.duracion_minutos, s.generado_por_ia;

CREATE VIEW vista_unidad_detalle AS
SELECT
    u.id AS unidad_id,
    u.titulo AS unidad_titulo,
    u.fecha_inicio,
    u.fecha_fin,
    u.contexto,
    g.nombre AS grado,
    a.nombre AS area,
    d.nombre AS docente,
    d.email AS docente_email,
    i.nombre AS institucion
FROM unidad u
JOIN grado g ON u.grado_id = g.id
JOIN area a ON u.area_id = a.id
JOIN docente d ON u.docente_id = d.id
JOIN institucion i ON d.institucion_id = i.id;

CREATE VIEW vista_sesion_actividades AS
SELECT
    s.id AS sesion_id,
    s.titulo AS sesion_titulo,
    a.tipo,
    GROUP_CONCAT(a.descripcion ORDER BY a.orden SEPARATOR '; ') AS actividades
FROM sesion s
LEFT JOIN actividad a ON s.id = a.sesion_id
GROUP BY s.id, s.titulo, a.tipo;

-- Nota de modelado:
-- No se agregan triggers en esta version porque la integridad principal
-- se cubre con claves foraneas, restricciones CHECK y validaciones
-- explicitas desde el backend.

-- ========================================
-- DATOS DE PRUEBA
-- ========================================

-- primaria: https://www.minedu.gob.pe/curriculo/pdf/programa-curricular-educacion-primaria.pdf

-- secundaria: https://www.ugelsanchezcarrion.gob.pe/wordpress/wp-content/uploads/2019/06/programa-secundaria-17-abril.pdf

INSERT INTO nivel_educativo (id, nombre) VALUES
    (1, 'Primaria'),
    (2, 'Secundaria'),
    (3, 'Inicial');

INSERT INTO ciclo (id, nombre) VALUES
    ('III', 'Ciclo III'),
    ('IV', 'Ciclo IV'),
    ('V', 'Ciclo V'),
    ('VI', 'Ciclo VI'),
    ('VII', 'Ciclo VII');

INSERT INTO grado (id, nivel_id, ciclo_id, nombre) VALUES
    (1, 1, 'III', '1ro'),
    (2, 1, 'III', '2do'),
    (3, 1, 'IV', '3ero'),
    (4, 1, 'IV', '4to'),
    (5, 1, 'V', '5to'),
    (6, 1, 'V', '6to'),
    (7, 2, 'VI', '1ro'),
    (8, 2, 'VI', '2do'),
    (9, 2, 'VII', '3ero'),
    (10, 2, 'VII', '4to'),
    (11, 2, 'VII', '5to');

INSERT INTO area (id, nombre) VALUES
    (1, 'Matematica'),
    (2, 'Comunicacion'),
    (3, 'Ciencia y Tecnologia');

INSERT INTO competencia (id, area_id, descripcion) VALUES
    (1, 1, 'Resuelve problemas de cantidad'),
    (2, 1, 'Resuelve problemas de forma, movimiento y localizacion'),
    (3, 2, 'Escribe diversos tipos de textos en su lengua materna'),
    (4, 3, 'Indaga mediante metodos cientificos para construir conocimientos');

INSERT INTO capacidad (id, competencia_id, descripcion) VALUES
    (1, 1, 'Traduce cantidades a expresiones numericas'),
    (2, 1, 'Usa estrategias y procedimientos de estimacion y calculo'),
    (3, 2, 'Modela objetos con formas geometricas y sus transformaciones'),
    (4, 3, 'Adecua el texto a la situacion comunicativa'),
    (5, 3, 'Organiza y desarrolla las ideas de forma coherente y cohesionada'),
    (6, 4, 'Problematiza situaciones para hacer indagacion');

INSERT INTO desempeno (id, grado_id, competencia_id, descripcion) VALUES
    (1, 2, 1, 'Establece equivalencias entre fracciones y explica sus procedimientos usando representaciones concretas y simbolicas'),
    (2, 2, 1, 'Resuelve problemas cotidianos que implican comparar y ordenar fracciones'),
    (3, 2, 2, 'Describe trayectorias y posiciones usando referentes espaciales'),
    (4, 3, 3, 'Redacta textos argumentativos considerando proposito, destinatario y estructura'),
    (5, 3, 3, 'Revisa y mejora la coherencia y cohesion de sus textos'),
    (6, 3, 4, 'Formula preguntas investigables sobre situaciones de su entorno');

INSERT INTO estandar_aprendizaje (id, competencia_id, ciclo_id, descripcion) VALUES
    (1, 1, 'III', 'Resuelve problemas referidos a acciones de juntar, separar, agregar, quitar, igualar y comparar cantidades usando estrategias de calculo y representaciones.'),
    (2, 3, 'IV', 'Escribe diversos tipos de textos de forma reflexiva, adecuando su texto al destinatario y proposito a partir de su experiencia previa.');

INSERT INTO desempeno_capacidad (desempeno_id, capacidad_id) VALUES
    (1, 1),
    (1, 2),
    (2, 2),
    (4, 4),
    (5, 5);

INSERT INTO estandar_desempeno (estandar_id, desempeno_id) VALUES
    (1, 1),
    (1, 2),
    (2, 4),
    (2, 5);

INSERT INTO institucion (id, nombre) VALUES
    (1, 'IE Jose Maria Arguedas'),
    (2, 'IE Maria Parado de Bellido');

INSERT INTO rol (id, nombre, descripcion) VALUES
    (1, 'ADMIN', 'Administrador con acceso a ingesta, catalogos y gestion'),
    (2, 'PROFESOR', 'Docente con acceso a planificacion y sesiones');

-- Passwords de prueba: Admin1234* y Demo1234*. Cambiar antes de usar en un entorno real.
INSERT INTO docente (id, nombre, email, institucion_id, rol_id, password_hash, activo, created_at) VALUES
    (1, 'Ana Torres Quispe', 'ana.torres@demo.edu.pe', 1, 2, '{bcrypt}$2a$10$d0y7ofACKEHfGIwTRyyW8er.k3qnCy4yasGey0xgu4w7Qt/RSYO0K', TRUE, '2026-04-01 08:00:00'),
    (2, 'Luis Rojas Paredes', 'luis.rojas@demo.edu.pe', 2, 2, '{bcrypt}$2a$10$d0y7ofACKEHfGIwTRyyW8er.k3qnCy4yasGey0xgu4w7Qt/RSYO0K', TRUE, '2026-04-01 08:30:00'),
    (3, 'Administrador', 'admin@demo.edu.pe', NULL, 1, '{bcrypt}$2a$10$zrX7Z6mAa5WzvmaiVwG77ukt5t1Q/1qgwUOoeHkE84TECh/vz5ROq', TRUE, '2026-04-01 07:30:00');

INSERT INTO unidad (id, titulo, grado_id, area_id, docente_id, fecha_inicio, fecha_fin, contexto, created_at) VALUES
    (1, 'Fracciones en la vida cotidiana', 2, 1, 1, '2026-04-01', '2026-04-30', 'Aula multigrado con estudiantes que relacionan las fracciones con situaciones de compra y reparto', '2026-04-01 09:00:00'),
    (2, 'Produccion de textos argumentativos', 3, 2, 2, '2026-04-05', '2026-05-05', 'Estudiantes de contexto urbano que debaten sobre el uso responsable de redes sociales', '2026-04-05 09:00:00');

INSERT INTO sesion (id, unidad_id, titulo, proposito, duracion_minutos, fecha, generado_por_ia, created_at) VALUES
    (1, 1, 'Comparamos fracciones equivalentes', 'Que los estudiantes representen, comparen y expliquen fracciones equivalentes usando material concreto y situaciones de reparto.', 90, '2026-04-10', TRUE, '2026-04-08 10:00:00'),
    (2, 1, 'Ordenamos fracciones en contextos cotidianos', 'Que los estudiantes ordenen fracciones en situaciones vinculadas a compras y recetas familiares.', 90, '2026-04-12', FALSE, '2026-04-09 10:00:00'),
    (3, 2, 'Escribimos textos argumentativos sobre el uso de redes sociales', 'Que los estudiantes elaboren un texto argumentativo coherente defendiendo una postura sobre el uso responsable de redes sociales.', 120, '2026-04-15', TRUE, '2026-04-11 11:00:00');

INSERT INTO sesion_competencia (id, sesion_id, competencia_id) VALUES
    (1, 1, 1),
    (2, 2, 1),
    (3, 3, 3);

INSERT INTO sesion_capacidad (id, sesion_id, capacidad_id) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 2, 2),
    (4, 3, 4),
    (5, 3, 5);

INSERT INTO sesion_desempeno (id, sesion_id, desempeno_id) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 2, 2),
    (4, 3, 4),
    (5, 3, 5);

INSERT INTO criterio_evaluacion (id, sesion_id, descripcion) VALUES
    (1, 1, 'Explica con claridad la equivalencia entre fracciones usando representaciones diversas'),
    (2, 1, 'Justifica el procedimiento utilizado para comparar fracciones'),
    (3, 2, 'Ordena fracciones correctamente a partir de situaciones contextualizadas'),
    (4, 3, 'Presenta argumentos pertinentes y bien organizados en su texto');

INSERT INTO evidencia (id, sesion_id, descripcion) VALUES
    (1, 1, 'Ficha de trabajo con equivalencias de fracciones y explicacion escrita'),
    (2, 2, 'Resolucion de problemas de comparacion y orden de fracciones'),
    (3, 3, 'Texto argumentativo final revisado y mejorado');

INSERT INTO instrumento_evaluacion (id, sesion_id, tipo, contenido_json) VALUES
    (1, 1, 'rubrica', JSON_OBJECT(
        'detalle', JSON_ARRAY(
            'Representa fracciones equivalentes con dibujos o material concreto',
            'Argumenta con claridad la equivalencia encontrada'
        )
    )),
    (2, 2, 'lista_cotejo', JSON_OBJECT(
        'detalle', JSON_ARRAY(
            'Identifica correctamente la fraccion mayor y menor',
            'Ordena fracciones respetando el contexto del problema'
        )
    )),
    (3, 3, 'rubrica', JSON_OBJECT(
        'detalle', JSON_ARRAY(
            'Presenta una postura clara',
            'Sustenta con razones y ejemplos',
            'Mantiene coherencia y cohesion textual'
        )
    ));

INSERT INTO actividad (id, sesion_id, tipo, descripcion, orden) VALUES
    (1, 1, 'inicio', 'Recuperan saberes previos a partir de situaciones de reparto de pizzas y frutas.', 1),
    (2, 1, 'desarrollo', 'Representan fracciones equivalentes con tiras de papel y material concreto.', 2),
    (3, 1, 'desarrollo', 'Explican en parejas por que dos fracciones son equivalentes.', 3),
    (4, 1, 'cierre', 'Socializan conclusiones y elaboran una idea fuerza sobre equivalencia de fracciones.', 4),
    (5, 2, 'inicio', 'Observan anuncios de ofertas y comentan cantidades fraccionarias en productos.', 1),
    (6, 2, 'desarrollo', 'Resuelven problemas contextualizados de comparacion y orden de fracciones.', 2),
    (7, 2, 'cierre', 'Reflexionan sobre como usan las fracciones en casa o en su comunidad.', 3),
    (8, 3, 'inicio', 'Analizan opiniones opuestas sobre el uso de redes sociales en adolescentes.', 1),
    (9, 3, 'desarrollo', 'Planifican y redactan un texto argumentativo con tesis, argumentos y conclusion.', 2),
    (10, 3, 'desarrollo', 'Revisan en parejas la coherencia y cohesion del texto producido.', 3),
    (11, 3, 'cierre', 'Comparten aprendizajes y establecen compromisos sobre el uso responsable de redes sociales.', 4);

INSERT INTO documento_curriculo (
    id,
    tipo,
    nombre_archivo,
    archivo_url,
    checksum_sha256,
    estado,
    error_detalle,
    fecha_subida,
    fecha_procesado
) VALUES
    (
        1,
        'curriculo',
        'curriculo_nacional_ebr.pdf',
        'https://www.minedu.gob.pe/curriculo/pdf/curriculo-nacional-de-la-educacion-basica.pdf',
        NULL,
        'PENDIENTE',
        NULL,
        '2026-04-02 12:00:00',
        NULL
    ),
    (
        2,
        'programa',
        'programa_curricular_secundaria.pdf',
        'https://www.minedu.gob.pe/curriculo/pdf/programa-curricular-educacion-secundaria.pdf',
        NULL,
        'PROCESADO',
        NULL,
        '2026-04-03 12:00:00',
        '2026-04-03 13:15:00'
    );
