### Decisiones de Diseño

**1. Sistema de Actas y Registros Académicos**
Para mantener un historial claro y escalable del progreso de los alumnos, se dividió el registro en tres entidades principales:

- **`GradeSheet` (Acta de Cursado):** Representa el acta general de una materia para un año lectivo específico. 
Actúa como cabecera para agrupar el historial de cursada de todos los estudiantes en ese período.
- **`Status` (Estado de Inscripción):** Funciona como el detalle individual (o "renglón") dentro de un `GradeSheet`. 
Registra la condición inicial y la condición final obtenida por cada estudiante al terminar la cursada.
- **`FinalSheet` (Acta de Examen Final):** Representa el acta formal de un llamado a examen final particular, 
donde se asientan las notas definitivas de los estudiantes que se presentaron a rendir.
- **`FinalGrade` (Examen Final individual):** Representa la nota del examen final de un alumno particular 
en FinalSheet específico.



**2. Gestión de Correlatividades (Clase `Condicion`)**

- **Decisión:** Se centralizó la lógica de requisitos en una única clase `Condicion`, 
incorporando dos atributos distintos: uno que define el estado requerido de la correlativa 
para **cursar** la materia actual, y otro para la condición requerida para **rendir** el final.
- **Justificación:** Esta estructura responde al ciclo de vida lógico del régimen académico. 
Si el sistema exige que una correlativa esté "Regular" o "Aprobada" para habilitar la cursada, 
seguramente exigirá que esté "Aprobada" para habilitar el examen final. 
Consolidar ambos atributos en la misma clase evita la redundancia de crear entidades separadas 
para cursado y finales, simplificando las consultas SQL a la hora de validar inscripciones.
