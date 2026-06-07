package com.is1.proyecto.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User;
import com.is1.proyecto.utils.EmailSender;
import com.is1.proyecto.utils.PasswordGenerator;

import org.javalite.activejdbc.Base;

/** Rutas --------------------------
 * /teacher/create (GET/POST), 
 * /teacher/assign (GET/POST), 
 * /teacher/unassign (GET/POST), 
 * /teacher/delete (GET/POST), 
 * /api/teachers/search (GET).
 */

public class TeacherService {
    public static String createTeacher(String firstName, String lastName, String dni, Integer fileCode, String degree, String email){
        try {
            Base.openTransaction();
            
            Person p = Person.findFirst("dni = ?", dni);
            if (p == null) {
                p = new Person();
                p.set("first_name", firstName);
                p.set("last_name", lastName);
                p.set("dni", dni);
                p.set("email", email);
                p.saveIt();
            } else {
                Teacher existing = Teacher.findFirst("person_id = ?", p.getId());
                if (existing != null) {
                    Base.rollbackTransaction();
                    String errorMsg = "Esta persona ya está registrada como profesor.";
                    return errorMsg;
                }
                p.set("email", email); // Actualizar mail por las dudas
                p.saveIt();
            }

            Teacher t = new Teacher();
            t.set("person_id", p.getId());
            t.set("degree", degree);
            t.set("file_code", fileCode);
            t.saveIt();

            // Solo si no tiene un usuario previamente
            User u = User.findFirst("person_id = ?", p.getId());
            String randomPassword = PasswordGenerator.generateSecurePassword(12);
            boolean isNewUser = (u == null);

            if (isNewUser) {
                u = new User();
                String hashedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt());
                u.set("name", dni);
                u.set("password", hashedPassword);
                u.set("person_id", p.getId());
                u.set("is_admin", 0);
                u.saveIt();
            }

            Base.commitTransaction();

            final boolean finalIsNewUser = isNewUser;
            final String finalPassword = randomPassword;
            final String finalEmail = email;

            CompletableFuture.runAsync(() -> {
                try {
                    if (finalIsNewUser) {
                        EmailSender.sendGenericAccountCreationMail(finalEmail, dni, firstName, lastName, finalPassword);
                    } else {
                        EmailSender.sendTeacherRoleAddedMail(finalEmail, firstName, lastName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return null;

        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();

            String errorMsg = "Error interno al procesar el alta.";
            return errorMsg;
        }
    }

    public static String assignTeacher(String teacherId, String subjectId){
        try {
            Teacher t = Teacher.findById(Integer.parseInt(teacherId));
            Subject s = Subject.findById(Integer.parseInt(subjectId));
            if (t != null && s != null) {
                t.add(s); //funciona por el Many@Many que puse en Teacher     
                return null;
            } else {
                return "Error: Profesor o Materia no encontrados";
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("unique") || errorMsg.contains("primary key")) {
                return "Ese profesor ya está asignado a esa materia.";
            } else {
                e.printStackTrace();
                return "Error interno al asignar.";
            }
        }
    }

    public static List<Teacher> findTeacher(String searchQuery){
        List<Teacher> teachers;

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String likeQuery = "%" + searchQuery.trim() + "%";
            teachers = Teacher.findBySQL(
                "SELECT t.* FROM teachers t JOIN persons p ON t.person_id = p.id WHERE p.first_name LIKE ? OR p.last_name LIKE ?", 
                likeQuery, likeQuery
            ).include(Person.class);
        }else{
            teachers = java.util.Collections.emptyList();
        }

        return teachers;
    }

    public static List<Map<String, Object>> searchTeachers(String query) {
        //Retornar lista vacia si no hay parámetro de búsqueda
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(); 
        }
        String likeQuery = "%" + query.trim() + "%";
        
        List<Teacher> teachers = Teacher.findBySQL(
            "SELECT t.* FROM teachers t JOIN persons p ON t.person_id = p.id WHERE p.first_name LIKE ? OR p.last_name LIKE ?", 
            likeQuery, likeQuery
        ).include(Person.class); 

        //Poner resultados en una lista simple para el JSON
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Teacher t : teachers) {
            resultList.add(Map.of(
                "id", t.getId(),
                "fullName", t.getFullNameString(),
                "dni", t.getDNI()
            )); 
        }
        
        return resultList;
    }


    public static List<Subject> getAllSubjects(){
        return Subject.findAll();
    }


    public static String unassignTeacher(String teacherIdStr, String subjectIdStr){
        try {
            Integer teacherId = Integer.parseInt(teacherIdStr);
            Integer subjectId = Integer.parseInt(subjectIdStr);

            Subject subject = Subject.findById(subjectId);
            if (subject == null) {
                return "La materia seleccionada no existe.";
            }

            // Evitar desasignar al profesor responsable de la materia
            if (subject.getInteger("responsible_id").equals(teacherId)) {
                return "No puedes desasignar a este profesor porque figura como el Responsable obligatorio de la materia. Asigna a otro responsable antes de removerlo.";
            }

            // PROCESO DE DESASIGNACIÓN
            Base.openTransaction();
            
            int rowsDeleted = Base.exec("DELETE FROM teaches WHERE teacher_id = ? AND subject_id = ?", teacherId, subjectId);
            
            if (rowsDeleted > 0) {
                Base.commitTransaction();
                return null;
            } else {
                Base.rollbackTransaction();
                return "El profesor ingresado no pertenece al equipo docente asignado a esta materia.";
            }

        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            return "Ocurrió un error inesperado al procesar la desasignación.";
        }
    }

    



    // GET: lista de profesores (filtrada o todos)
    public static List<Teacher> getTeachersForDeletion(String query, int offset) {
        if (query != null && !query.trim().isEmpty()) {
            return Teacher.findBySQL(
                "SELECT t.* FROM teachers t " +
                "JOIN persons p ON t.person_id = p.id " +
                "WHERE p.first_name LIKE ? OR p.last_name LIKE ? OR CAST(p.dni AS TEXT) LIKE ?",
                "%" + query.trim() + "%", 
                "%" + query.trim() + "%", 
                "%" + query.trim() + "% LIMIT " + offset
            ).include(Person.class);
        } else {
            return Teacher.findAll().include(Person.class);
        }
    }

    // POST
    public static String deleteTeacher(String teacherId) {
        if (teacherId == null || teacherId.isEmpty())
            return "ID de docente no proporcionado.";
        
        try {
            Base.openTransaction();
            Teacher t = Teacher.findById(Integer.parseInt(teacherId));
            
            if (t != null) {
                t.delete();
                Base.commitTransaction();
                return null;
            } else {
                Base.rollbackTransaction();
                return "El docente no existe.";
            }
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            return "Error interno al eliminar el docente.";
        }
    }

}
