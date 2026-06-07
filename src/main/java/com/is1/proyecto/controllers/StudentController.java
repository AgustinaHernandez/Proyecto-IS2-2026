package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import spark.Request;
import spark.Response;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.services.StudentService;
import com.is1.proyecto.utils.EmailSender;
import com.is1.proyecto.utils.PasswordGenerator;

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
        if (firstname == null || firstname.isEmpty()
            || lastname == null || lastname.isEmpty() || email == null || email.isEmpty()
            || dniStr == null || dniStr.isEmpty()
        ) {
            String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + errorMsg);
            return "";
        }
        //Validación de nombre
        String result = firstname.replaceAll("\\d", ""); //Quitar todos los números del firstname
        if(result.length() != firstname.length()){ //Chequear si cambió la longitud
            String errorMsg = URLEncoder.encode("El nombre no puede contener números.", StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + errorMsg);
            return "";
        }
        //Validación de apellido
        result = lastname.replaceAll("\\d", ""); //Quitar todos los números del lastname
        if(result.length() != lastname.length()){ //Chequear si cambió la longitud
            String errorMsg = URLEncoder.encode("El apellido no puede contener números.", StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + errorMsg);
            return "";
        }
        //Validación de mail
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if(!email.matches(emailRegex)) {
            String errorMsg = URLEncoder.encode("Ingrese un correo electrónico válido (ej: usuario@dominio.com).", StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + errorMsg);
            return "";
        }
        //Validación de DNI
        Integer dni = 0;
        try {
            dni = Integer.parseInt(dniStr);
            if (dni <= 0) throw new IllegalArgumentException("DNI inválido");
        } catch (Exception e) {
            res.status(400);
            String errorMsg = URLEncoder.encode("El DNI debe ser un número válido.", StandardCharsets.UTF_8);
            res.redirect("/student/create?error=" + errorMsg);
            return "";
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
}