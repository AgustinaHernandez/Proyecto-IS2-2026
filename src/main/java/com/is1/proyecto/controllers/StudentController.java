package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import spark.Request;
import spark.Response;

import com.is1.proyecto.services.StudentService;

import spark.ModelAndView;

/** Rutas --------------------------
 * /student/create (GET/POST), 
 * /student/new (GET/POST), 
 * /student/delete (GET/POST).
 */
public class StudentController {
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