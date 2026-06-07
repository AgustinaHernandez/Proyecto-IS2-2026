--- Inserts para pruebas manuales ------------------------------------------------

DELETE FROM final_grades;
DELETE FROM final_sheets;
DELETE FROM statuses;
DELETE FROM grade_sheets;
DELETE FROM conditions;
DELETE FROM enrolled_subject;
DELETE FROM enrolled_plan;

DELETE FROM subject_belongs_plan;
DELETE FROM plans;
DELETE FROM careers;
DELETE FROM teaches;
DELETE FROM subjects;
DELETE FROM teachers;
DELETE FROM users;
DELETE FROM persons;

INSERT INTO persons (dni, first_name, last_name, email) VALUES 
(10000000, 'Admin', 'Admin', 'admin@admin.com'),
(11111111, 'Fran', 'P', 'fran@test.com'),
(22222222, 'Agus', 'H', 'agus@test.com'),
(33333333, 'Adrian', 'B', 'adrian@test.com'),
(44444444, 'Jose', 'M', 'jose@test.com'),
(55555555, 'Hose', 'Ignacion Mendez', 'hose@test.com'),
(66666666, 'Santiago', 'Hernandez', 'santiago@test.com'),
(77777777, 'Francisco', 'Franco P', 'francisco@test.com');

INSERT INTO users (person_id, name, password, is_admin) VALUES 
(1, 'admin', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 1),
(2, 'fran', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
(3, 'agus', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 1),
(4, 'adrian', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
(5, 'jose', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
(6, 'hose', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
(7, 'santiago', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0),
(8, 'francisco', '$2a$10$8uG3r/WPfQn6IbwQm.d0peKotH8Wt49OaDjPcjVQplM/6TYyUiVhq', 0);


INSERT INTO teachers (person_id, file_code, degree) VALUES 
(2, 0001, 'LICENCIATURA'),
(6, 0002, 'LICENCIATURA'),
(7, 0003, 'LICENCIATURA');


INSERT INTO subjects (code, name, responsible_id) VALUES 
(101, 'Programación I', 1),
(102, 'Ingeniería de Software I', 3),
(103, 'Sistemas Operativos', 2),
(104, 'Bases de Datos I', 1),
(105, 'Programación II', 2),
(106, 'Redes y Comunicaciones', 3),
(107, 'Ingeniería de Software II', 1),
(108, 'Inteligencia Artificial', 2);

INSERT INTO careers (name) VALUES 
('Licenciatura en Ciencias de la Computación'),
('Profesorado en Ciencias de la Computación'),
('Analista en Computación');

INSERT INTO plans (career_id, state, version) VALUES 
(1, 'VIGENTE', 2024),
(2, 'SUSPENDIDO', 2018);

INSERT INTO subject_belongs_plan (subject_id, plan_id) VALUES 
(1, 1),
(2, 1),
(3, 1),
(1, 2),
(4, 1),
(5, 1),
(6, 1),
(7, 1),
(8, 1),
(1, 2);


INSERT INTO students (person_id) VALUES 
(3),
(4),
(5);

INSERT INTO enrolled_plan (student_id, plan_id) VALUES 
(1, 1),
(2, 1),
(3, 1);


INSERT INTO conditions (subject_id, correlative_id, course_condition, exam_condition) VALUES 
(2, 1, 'REGULAR', 'APROBADA'),
(3, 1, 'APROBADA', 'APROBADA'),
(3, 2, 'REGULAR', 'APROBADA'),
(4, 1, 'REGULAR', 'APROBADA'),
(5, 1, 'APROBADA', 'APROBADA'),
(6, 3, 'REGULAR', 'APROBADA'),
(7, 2, 'REGULAR', 'APROBADA'),
(8, 5, 'APROBADA', 'APROBADA');


INSERT INTO grade_sheets (subject_id, year) VALUES 
(1, 2023),
(2, 2024),
(3, 2024),
(4, 2024),
(1, 2024),
(2, 2026),
(6, 2026),
(5, 2026),
(3, 2026);

INSERT INTO statuses (grade_sheet_id, student_id, initial_condition, final_condition) VALUES 
(1, 1, 'INSCRIPTO', 'APROBADO'),    -- Años anteriores
(1, 2, 'INSCRIPTO', 'REGULAR'),
(3, 1, 'INSCRIPTO', 'APROBADO'),
(4, 1, 'INSCRIPTO', 'REGULAR'),
(1, 2, 'INSCRIPTO', 'REGULAR'),
(2, 2, 'INSCRIPTO', 'REGULAR'),
(5, 3, 'INSCRIPTO', 'APROBADO'),
(6, 1, 'INSCRIPTO', 'INSCRIPTO'),   -- Año actual
(7, 1, 'INSCRIPTO', 'INSCRIPTO'),
(8, 2, 'INSCRIPTO', 'INSCRIPTO'),
(9, 3, 'INSCRIPTO', 'INSCRIPTO');


INSERT INTO enrolled_subject (student_id, subject_id) VALUES 
(3, 1);

INSERT INTO final_sheets (subject_id, year, call) VALUES 
(1, '2023-12-15', 'Primero'),
(4, '2024-12-15', 'Primero');

INSERT INTO final_grades (final_sheet_id, student_id, grade) VALUES 
(2, 1, 8),
(1, 2, 7);