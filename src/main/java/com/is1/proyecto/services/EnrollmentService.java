package com.is1.proyecto.services;

import java.time.Year;
import java.util.List;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.User;

/** Rutas --------------------------
 * /student/subjetc-enroll (GET/POST).
 */
public class EnrollmentService {

    // DTO
    public static class AvailableSubjectsResult {
        public boolean success;
        public String errorMessage;
        public List<Subject> subjects;
    }


    // GET: obtener las materias
    public static AvailableSubjectsResult getAvailableSubjects(Object userId){
        AvailableSubjectsResult res = new AvailableSubjectsResult();
        res.success = false;

        User user = User.findById(userId);
        if (user == null) {
            res.errorMessage = "Usuario no encontrado.";
            return res;
        }
        
        Student student = Student.findFirst("person_id = ?", user.get("person_id"));
        if (student == null) {
            res.errorMessage = "Error interno: No se encontró un perfil de estudiante para tu cuenta.";
            return res;
        }

        res.success = true;
        res.errorMessage = null;
        res.subjects = student.getAvailableSubjectsToEnroll();
        return res;
    }


    // POST: Incripcion en la bd
    public static String enrollInSubject(Long userId, String subjectIdParam) {
        if (subjectIdParam == null || subjectIdParam.isEmpty()) {
            return "Selección de materia inválida.";
        }

        try {
            int currentYear = Year.now().getValue();
            User user = User.findById(userId);
            Student student = (user != null) ? Student.findFirst("person_id = ?", user.get("person_id")) : null;
            Subject subject = Subject.findById(subjectIdParam);

            if (student == null || subject == null) {
                return "Error de consistencia: Alumno o materia no encontrados.";
            }

            List<String> errors = student.validateEnrollment(subject);
            if (!errors.isEmpty()) {
                return "Inscripción rechazada: " + String.join(" ", errors);
            }

            Base.openTransaction();

            String gsSql = "SELECT id FROM grade_sheets WHERE subject_id = ? AND year = ? LIMIT 1";
            Object gradeSheetId = Base.firstCell(gsSql, subject.getId(), currentYear);

            if (gradeSheetId == null) {
                Base.exec("INSERT INTO grade_sheets (subject_id, year) VALUES (?, ?)", subject.getId(), currentYear);
                //Recuperamos el ID que se le acaba de asignar
                gradeSheetId = Base.firstCell(gsSql, subject.getId(), currentYear);
            }

            Base.exec("INSERT INTO statuses (grade_sheet_id, student_id, initial_condition, final_condition) VALUES (?, ?, ?, ?)", 
                      gradeSheetId, student.getId(), "INSCRIPTO", "INSCRIPTO");

            Base.commitTransaction();
            return null; // todo ok, ou llea B)
            
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            return "Ocurrió un error inesperado en el servidor al procesar el alta.";
        }
    }



}