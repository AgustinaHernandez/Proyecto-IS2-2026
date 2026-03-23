--- Inserts para pruebas manuales ------------------------------------------------

-- limpiar datos
DELETE FROM teachers;
DELETE FROM users;
DELETE FROM persons;
DELETE FROM enrolled_subject;
DELETE FROM enrolled_plan;
DELETE FROM teaches;
DELETE FROM subject_belongs_plan;
DELETE FROM subjects;
DELETE FROM plans;
DELETE FROM careers;
DELETE FROM students;


-- Insertar Personas
INSERT INTO persons (dni, first_name, last_name) VALUES 
(10000000, 'Admin', 'Admin'),
(11111111, 'Fran', 'P'),
(22222222, 'Agus', 'H'),
(33333333, 'Adrian', 'B'),
(44444444, 'Jose', 'M'),
(55555555, 'Hose', 'Ignacion Mendez'),
(66666666, 'Santiago', 'Hernandez'),
(77777777, 'Francisco', 'Franco P');

-- Insertar Usuarios de login
    -- Todos con contraseña hasehada "admin"
INSERT INTO users (name, password, is_admin) VALUES 
('admin', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 1),
('fran', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
('agus', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 1),
('adrian', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
('jose', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
('hose', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
('santiago', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
('francisco', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0);


-- Insertar profesores
INSERT INTO teachers (person_id, degree, email) VALUES
(2, 'LICENCIATURA', 'fran@cs.com'),
(6, 'LICENCIATURA', 'hose@cs.com'),
(7, 'LICENCIATURA', 'santiago@cs.com');

-- Insertar estudiantes (convertir las personas en estudiantes)
INSERT INTO students (person_id) VALUES 
(3), -- agus
(4), -- adrian
(5), -- jose
(8); -- francisco


---  Inserts de estructuras académicas -------------------------------------------

-- Insertar materias
INSERT INTO subjects (code, name, responsible_id) VALUES 
(101, 'Programación I', 1),
(102, 'Ingeniería de Software I', 3),
(103, 'Sistemas Operativos', 2);

-- Insertar Carreras
INSERT INTO careers (id, name) VALUES 
(1, 'Analista en Computación'),
(2, 'Lic. en Computación'),
(3, 'Prof. en Computación');

-- Insertar Planes
INSERT INTO plans (career_id, version, status) VALUES 
(1, 2019, "VIGENTE"),
(2, 2020, "VIGENTE"),
(3, 2015, "VIGENTE");

-- Vincular Materias a los Planes
INSERT INTO subject_belongs_plan (subject_id, plan_id) VALUES 
(1, 1), (2, 1), (3, 1), -- Analista
(1, 2), (2, 2), (3, 2), -- Licenciatura
(1, 3);                 -- Profesorado


--- Inserts de Roles ----------------------------------------------------------

-- Asignacion de docentes a materias
INSERT INTO teaches (teacher_id, subject_id) VALUES 
(2, 1), -- Hose, Programacion I
(3, 1), -- Santiago, en Programacion I
(1, 2), -- Fran, en Ing. de Software I
(3, 3); -- Santiago, Sistemas Operativos


--- Inscripciones de Alumnos ------------------------------------------------

-- Inscribir Estudiantes a los Planes
INSERT INTO enrolled_plan (student_id, plan_id) VALUES 
(1, 2), -- Agus Licenciatura
(2, 2), -- Adrian Licenciatura
(3, 1), -- Jose Analista
(4, 3); -- Francisco Profesorado

-- Inscribir Estudiantes a las Materias
INSERT INTO enrolled_subject (student_id, subject_id) VALUES 
(1, 2),
(1, 3),
(2, 1),
(3, 1),
(4, 1);

