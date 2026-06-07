package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.services.TeacherService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/** Rutas --------------------------
 * /teacher/create (GET/POST), 
 * /teacher/assign (GET/POST), 
 * /teacher/unassign (GET/POST), 
 * /teacher/delete (GET/POST), 
 * /api/teachers/search (GET).
 */

public class TeacherController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    //GET teacher/create
    public static ModelAndView renderTeacherCreation(Request req, Response res){
        Map<String, Object> model = Map.of(
            "tituloPagina", "Alta de profesor",
            "errorMessage", req.queryParamOrDefault("error", ""),
            "successMessage", req.queryParamOrDefault("success", "")
        );
            
        return new ModelAndView(model, "teacher_form.mustache");
    }

    //POST teacher/create
    public static Object handleTeacherCreation(Request req, Response res){
        String firstname = req.queryParams("firstname").trim();
        String lastname = req.queryParams("lastname").trim();
        String dniStr = req.queryParams("dni").trim();
        String email = req.queryParams("email").trim();
        String fileCodeStr = req.queryParams("file_code").trim();
        String degree = req.queryParams("degree").trim();

        if (firstname == null || firstname.isEmpty() || lastname == null || lastname.isEmpty() || 
            email == null || email.isEmpty() || dniStr == null || dniStr.isEmpty() || degree == null || degree.isEmpty()) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8));
            return "";
        }

        // Validaciones de formato
        if (!firstname.replaceAll("\\d", "").equals(firstname)) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode("El nombre no puede contener números.", StandardCharsets.UTF_8));
            return "";
        }
        if (!lastname.replaceAll("\\d", "").equals(lastname)) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode("El apellido no puede contener números.", StandardCharsets.UTF_8));
            return "";
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode("Ingrese un correo electrónico válido.", StandardCharsets.UTF_8));
            return "";
        }

        Integer fileCode;
        Integer dni;
        try {
            dni = Integer.parseInt(dniStr);
            fileCode = Integer.parseInt(fileCodeStr);
            if (dni <= 0) throw new IllegalArgumentException();
            if (fileCode <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode("El DNI y/o Nro. de Legajo debe ser un número válido.", StandardCharsets.UTF_8));
            return "";
        }

        String errorMsg = TeacherService.createTeacher(firstname, lastname, dniStr, fileCode, degree, email);

        if (errorMsg != null) {
            res.redirect("/teacher/create?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        } else {
            res.redirect("/teacher/create?success=" + URLEncoder.encode("Docente dado de alta exitosamente.", StandardCharsets.UTF_8));
        }
        return "";
    }

    //GET teacher/assign
    public static ModelAndView renderTeacherAssign(Request req, Response res){
        List<Map<String, Object>> subjects = Subject.findAll().toMaps();
        // obtener el parámetro de búsqueda que el admin puso en el campo
        String searchQuery = req.queryParams("q");
        List<Teacher> teachers;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String likeQuery = "%" + searchQuery.trim() + "%";
            teachers = Teacher.findBySQL(
                "SELECT t.* FROM teachers t JOIN persons p ON t.person_id = p.id WHERE p.first_name LIKE ? OR p.last_name LIKE ?", 
                likeQuery, likeQuery
            ).include(Person.class);
        } else {
            teachers = java.util.Collections.emptyList();
        }
        Map<String, Object> model = Map.of(
            "tituloPagina", "Asignar Profesor a Materia",
            "teachers", teachers,
            "subjects", subjects, 
            "searchQuery", searchQuery != null ? searchQuery : "",
            "errorMessage", req.queryParamOrDefault("error", ""),
            "successMessage", req.queryParamOrDefault("success", "")
        );
        
        return new ModelAndView(model, "teacher_assign_form.mustache");
    }

    //POST teacher/assign
    public static Object handleTeacherAssignation(Request req, Response res){
        String id = req.queryParams("teacher_id");
        String subjectId = req.queryParams("subject_id");

        if (id == null || subjectId == null || id.isEmpty() || subjectId.isEmpty()) {
            return "Faltan datos obligatorios";
        }
        
        String errorMsg = TeacherService.assignTeacher(id, subjectId);

        if (errorMsg != null) {
            res.redirect("/teacher/assign?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        } else {
            res.redirect("/teacher/assign?success=" + URLEncoder.encode("Docente dado de alta exitosamente.", StandardCharsets.UTF_8));
        }
        return "";
    }

    //GET "API" para buscar profes
    public static Object handleSearchTeachersAPI(Request req, Response res) {
        //Configurar header para que el navegador sepa que es JSON
        res.type("application/json"); 
        
        String q = req.queryParams("q"); 

        List<Map<String, Object>> resultList = TeacherService.searchTeachers(q);

        try {
            //Convertir la lista a un String JSON
            return objectMapper.writeValueAsString(resultList); 
        } catch (Exception e) {
            e.printStackTrace();
            res.status(500);
            return "[]";
        }
    }
}
