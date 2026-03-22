-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL,           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
    is_admin INTEGER NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS persons;

-- Creación de tabla 'persons'
CREATE TABLE persons (
    id INTEGER NOT NULL PRIMARY KEY,
    dni INTEGER NOT NULL UNIQUE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL
);

DROP TABLE IF EXISTS teachers;

-- Creación de tabla 'teachers'
CREATE TABLE teachers (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    person_id INTEGER NOT NULL,
    degree TEXT NOT NULL,
    email TEXT NOT NULL,

    CONSTRAINT valid_degree CHECK (
        degree IN (
            'BACHILLERATO',
            'TECNICATURA',
            'PROFESORADO',
            'LICENCIATURA',
            'MAESTRIA',
            'DOCTORADO',
            'OTRO'
        )
    ),

    CONSTRAINT fk_id_teacher FOREIGN KEY (person_id) REFERENCES persons (id)
);

DROP TABLE IF EXISTS students;

CREATE TABLE students (
    id INTEGER NOT NULL PRIMARY KEY,
    person_id INTEGER NOT NULL,

    CONSTRAINT fk_id_student FOREIGN KEY (person_id) REFERENCES persons (id)
);

DROP TABLE IF EXISTS careers;

CREATE TABLE careers (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(25)

);

DROP TABLE IF EXISTS plans;

CREATE TABLE plans (
    id INTEGER NOT NULL PRIMARY KEY,
    career_id INTEGER NOT NULL,
    version INTEGER NOT NULL,
    
    CONSTRAINT fk_career FOREIGN KEY (career_id) REFERENCES careers (id)
);

DROP TABLE IF EXISTS subjects;

CREATE TABLE subjects (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    code INTEGER NOT NULL UNIQUE,
    name TEXT NOT NULL,
    responsible_id INTEGER NOT NULL,

    CONSTRAINT fk_responsible FOREIGN KEY (responsible_id) REFERENCES teachers (id)
);

DROP TABLE IF EXISTS subject_belongs_plan;

CREATE TABLE subject_belongs_plan(
    subject_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,

    CONSTRAINT fk_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT pk_pertenece PRIMARY KEY (plan_id, subject_id)
);

DROP TABLE IF EXISTS teaches;

CREATE TABLE teaches(
    teacher_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,

    CONSTRAINT fk_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE,
    CONSTRAINT fk_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT pk_teaches PRIMARY KEY (teacher_id, subject_id)
);

DROP TABLE IF EXISTS enrolled_subject;

CREATE TABLE enrolled_subject(
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,

    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT pk_enrolled PRIMARY KEY (student_id, subject_id)
);

DROP TABLE  IF EXISTS enrolled_plan;

CREATE TABLE enrolled_plan(
    student_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,

    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT pk_enrolled_plan PRIMARY KEY (student_id, plan_id)
);