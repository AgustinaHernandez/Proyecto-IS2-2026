package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.Request;
import spark.Response;

import com.is1.proyecto.models.Enrolled_Plan;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.User;
import com.is1.proyecto.services.StudentService;
import com.is1.proyecto.services.StudentService.PerformanceQueryResult;
import com.is1.proyecto.services.UserService;
import com.is1.proyecto.utils.InputValidator;

import spark.ModelAndView;

/** Rutas --------------------------
 * /student/create (GET/POST), 
 * /student/delete (GET/POST).
 */
public class StudentController {
    /**
     *  ---------------- STUDENT CREATE ------------------------------------------
    */

    //GET
    public static ModelAndView renderCreationForm(Request req, Response res){
        Map<String, Object> model = new HashMap<>();

        String successMessage = req.queryParams("success");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        return new ModelAndView(model, "student_form.mustache");
    }

    //POST
    public static Object handleStudentCreation(Request req, Response res){
        String firstname = req.queryParams("firstname").trim();
        String lastname = req.queryParams("lastname").trim();
        String dniStr = req.queryParams("dni").trim();
        String email = req.queryParams("email").trim();
        
        // Validaciones básicas: campos no pueden ser nulos o vacíos.
        String emptyFieldsError = InputValidator.checkNoEmptyFields(new String[]{firstname, lastname, dniStr, email});
        if(emptyFieldsError != null){
            res.redirect("/student/create?error=" + emptyFieldsError);
        }
        //Validación de nombre
        String firstNameValidationError = InputValidator.validateName(firstname);
        if(firstNameValidationError != null){
            res.redirect("/student/create?error=" + firstNameValidationError);
        }
        //Validación de apellido
        String lastNameValidationError = InputValidator.validateName(lastname);
        if(lastNameValidationError != null){
            res.redirect("/student/create?error=" + lastNameValidationError);
        }
        //Validación de mail
        String emailValidationError = InputValidator.validateEmail(email);
        if(emailValidationError != null){
            res.redirect("/recover-password?error=" + emailValidationError);
        }
        //Validación de DNI
        String dniValidationError = InputValidator.validateDNI(dniStr);
        if(dniValidationError != null){
            res.redirect("/recover-password?error=" + dniValidationError);
        }

        String errorMsg = StudentService.createStudent(firstname, lastname, dniStr, email);
        
        if(errorMsg != null){
            String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + encodedError);
        } else {
            String successMsg = URLEncoder.encode("Estudiante dado de alta exitosamente.", StandardCharsets.UTF_8);
            res.redirect("/student/create?success=" + successMsg);
        }
        return "Ocurrió un error interno al procesar la solicitud.";
    }


    /**
     *  ---------------- STUDENT DELETE ------------------------------------------
     */

    // GET
    public static ModelAndView renderDeleteForm(Request req, Response res){
        Map<String, Object> model = new HashMap<>(); 

        String successMessage = req.queryParams("successMessage");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        String errorMessage = req.queryParams("errorMessage");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        return new ModelAndView(model, "student_del.mustache");
    }

    // POST
    public static Object handleStudentDelete(Request req, Response res){
        String studentId = req.queryParams("id");
        String errorMsg = StudentService.deleteStudent(studentId);

        if(errorMsg != null){
            String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            res.redirect("/student/delete?errorMessage=" + encodedError);
        } else {
            String successMsg = URLEncoder.encode("Estudiante [" + studentId + "] dado de baja con éxito", StandardCharsets.UTF_8);
            res.redirect("/student/delete?successMessage=" + successMsg);
        }
        return "";
    }

    /**
     *  ---------------- ACADEMIC PERFORMANCE QUERY ------------------------------------------
     */

    // GET
    public static ModelAndView renderPerformanceForm(Request req, Response res){
        Map<String, Object> model = new HashMap<>();
        Object userId = req.session().attribute("userId");
        User user = User.findById(userId);
        Student student = Student.findFirst("person_id = ?", user.get("person_id"));

        List<Enrolled_Plan> enrolled = Enrolled_Plan.where("student_id = ?", student.getId()).include(Plan.class);

        model.put("enrolled", enrolled);
        return new ModelAndView(model, "enrolled_careers.mustache");
    }

    //POST
    public static ModelAndView handlePerformanceQuery(Request req, Response res){
        Map<String, Object> model = new HashMap<>();
        String planId = req.queryParams("plan_id");
        Object userId = req.session().attribute("userId");
        User user = UserService.find(userId);
        Object personId = user.get("person_id");
        String mode = req.queryParams("mode");

        String subjectsQuery = "SELECT s.code, s.name, fs.year, fg.grade " + 
                                "FROM (SELECT * FROM enrolled_plan WHERE plan_id = ? AND student_id = ?) AS ep " + //Subconsulta para plan seleccionado
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + //Materias del plan
                                "INNER JOIN enrolled_subject es ON es.student_id = ep.student_id AND sbp.subject_id = es.subject_id " + //Materias del alumno y del plan
                                "INNER JOIN subjects s ON es.subject_id = s.id " + //Materias (para sacar los datos)
                                "INNER JOIN final_sheets fs ON s.id = fs.subject_id " +  
                                "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id " + 
                                "WHERE fg.student_id = ?";

        PerformanceQueryResult query = StudentService.performanceQuery(planId, personId, mode, subjectsQuery);
        model.put("subjects", query.subjects);
        model.put("hasSubjects", !query.allSubjects.isEmpty());
        model.put("mode", mode);
        model.put("both", query.both);
        model.put("totalAverage", query.totalAverage);
        model.put("approvedAverage", query.approvedAverage);
        return new ModelAndView(model, "academic_performance.mustache");
    }

}