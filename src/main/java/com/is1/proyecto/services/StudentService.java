package com.is1.proyecto.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.User;
import com.is1.proyecto.utils.EmailSender;
import com.is1.proyecto.utils.PasswordGenerator;

/** Rutas --------------------------
 * /student/create (GET/POST), 
 * /student/new (GET/POST), 
 * /student/delete (GET/POST)
 */


public class StudentService {

    public static class PerformanceQueryResult {
        public List<Map> subjects;
        public List<Map> allSubjects;
        public float totalAverage;
        public float approvedAverage;
        public boolean both;
    }

    public static String createStudent(String firstName, String lastName, String dni, String email){
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
                Student existingStudent = Student.findFirst("person_id = ?", p.getId());
                if (existingStudent != null) {
                    Base.rollbackTransaction();
                    return "Esta persona ya está registrada como estudiante.";
                }
            }
            Student ac = new Student();
            ac.set("person_id", p.getId());
            ac.saveIt();
            User u = User.findFirst("name = ?", dni);
            String randomPassword = PasswordGenerator.generateSecurePassword(8);
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
            
            if (isNewUser) {
                // El mail original con credenciales
                try { EmailSender.sendGenericAccountCreationMail(email, dni, firstName, lastName, randomPassword); } 
                catch (Exception e) { e.printStackTrace(); }
            } else {
                // El usuario ya existía, le avisamos que le agregaron el perfil
                try { EmailSender.sendStudentRoleAddedMail(email, firstName, lastName); } 
                catch (Exception e) { e.printStackTrace(); }
            }

            return null;
        } catch (Exception e) {
            Base.rollbackTransaction(); 
            e.printStackTrace(); 
            
            return "Error interno al procesar el registro."; 
        }
    }

    /**
     *  ---------------- STUDENT DELETE ------------------------------------------
     */
    public static String deleteStudent(String studentId) {

        if(studentId == null || studentId.isEmpty()){
            return "Error: Faltan datos obligatorios";
        }

        try {
            Base.openTransaction();
            Student st = Student.findById(Integer.parseInt(studentId));
            if(st != null && st.delete()){
                Base.commitTransaction();
                return null; //Creación correcta

            } else {
                Base.rollbackTransaction();
                return "Error: Estudiante con código [" + studentId + "] no encontrado";
            }

        } catch(Exception e){
            Base.rollbackTransaction();
            e.printStackTrace();
            return "Error al eliminar el estudiante [" + studentId + "]";
        }
    }
    
    /**
     *  ---------------- ACADEMIC PERFORMANCE QUERY  ------------------------------------------
     */
    public static PerformanceQueryResult performanceQuery(String planId, Object personId, String mode){
        Student student = Student.findFirst("person_id = ?", personId);

        String baseQuery = "SELECT s.code, s.name, fs.year, fg.grade " + 
                           "FROM enrolled_plan ep " +
                           "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " +
                           "INNER JOIN subjects s ON sbp.subject_id = s.id " +
                           "INNER JOIN final_sheets fs ON s.id = fs.subject_id " +  
                           "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id AND fg.student_id = ep.student_id " + 
                           "WHERE ep.plan_id = ? AND ep.student_id = ? AND fg.grade IS NOT NULL";

        String gradeMode = "";
        boolean both = false;
        if(mode.equals("aprobadas")){
            gradeMode = " AND fg.grade >= 5";
        } else if (mode.equals("desaprobadas")){
            gradeMode = " AND fg.grade < 5";
        } else {
            both = true;
        }


        List<Map> subjects = Base.findAll(baseQuery + gradeMode, planId, student.getId());
        List<Map> allSubjects = Base.findAll(baseQuery, planId, student.getId());
        
        float totalAverage = 0;
        float approvedAverage = 0;
        int approvedSubjects = 0;
        for(Map m : allSubjects){
            Object rawGrade = m.get("grade");
            float grade = ((Number) rawGrade).floatValue();
            if(grade >= 5){
                approvedAverage += grade;
                approvedSubjects++;
            }
            totalAverage += grade;
        }

        if(!allSubjects.isEmpty()){
            totalAverage /= allSubjects.size();
        } //Si no hay materias, el promedio no se muestra, por ende, no importa cómo haya quedado
        if(approvedSubjects > 0){
            approvedAverage /= approvedSubjects;
        } else {
            approvedAverage = 0;
        }

        PerformanceQueryResult p = new PerformanceQueryResult();
        p.allSubjects = allSubjects;
        p.subjects = subjects;
        p.totalAverage = totalAverage;
        p.approvedAverage = approvedAverage;
        p.both = both;
        return p;
    }

    public static Student findFirstStudent(String subquery, Object id){
        return Student.findFirst(subquery, id);
    }
}
