package com.is1.proyecto.models.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.*;
import org.javalite.activejdbc.Base; 

public class TeacherController {
    
    public static void registrarRutas(MustacheTemplateEngine engine, ObjectMapper objectMapper) {

        /**
         *      Alta de profesor
         */
        get("/teacher/create", (req, res) -> {

            Map<String, Object> model = Map.of(
                "tituloPagina", "Alta de profesor",
                "errorMessage", req.queryParamOrDefault("error", ""),
                "successMessage", req.queryParamOrDefault("message", "")
            );
            
            return new ModelAndView(model, "teacher_form.mustache");
        }, engine);


        post("/teacher/new", (req, res) -> {
           String firstname = req.queryParams("firstname").trim();
           String lastname = req.queryParams("lastname").trim();
           String dniStr = req.queryParams("dni").trim();
           String email = req.queryParams("email").trim();
           String degree = req.queryParams("degree").trim();
          
            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (firstname == null || firstname.isEmpty()
                || lastname == null || lastname.isEmpty() || email == null || email.isEmpty()
                || dniStr == null || dniStr.isEmpty()  || degree == null || degree.isEmpty()
            ) {
               String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
               res.redirect("/teacher/create?error=" + errorMsg);
               return "";
            }
            //Validación de nombre
            String result = firstname.replaceAll("\\d", "");
            if(result.length() != firstname.length()){
                String errorMsg = URLEncoder.encode("El nombre no puede contener números.", StandardCharsets.UTF_8);
                res.redirect("/teacher/create?error=" + errorMsg);
                return "";
            }
            //Validación de apellido
            result = lastname.replaceAll("\\d", "");
            if(result.length() != lastname.length()){
                String errorMsg = URLEncoder.encode("El apellido no puede contener números.", StandardCharsets.UTF_8);
                res.redirect("/teacher/create?error=" + errorMsg);
                return "";
            }
            //Validación de mail
            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            if(!email.matches(emailRegex)) {
                String errorMsg = URLEncoder.encode("Ingrese un correo electrónico válido (ej: usuario@dominio.com).", StandardCharsets.UTF_8);
                res.redirect("/teacher/create?error=" + errorMsg);
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
                res.redirect("/teacher/create?error=" + errorMsg);
                return "";
            }

            //Principal
            try {
               Base.openTransaction();  // Iniciamos la transaccion
               Person p = new Person(); // Crea una nueva instancia del modelo Person.
               p.set("first_name", firstname);
               p.set("last_name", lastname);
               p.set("dni", dni);
               p.saveIt();
            
               Teacher ac = new Teacher(); // Crea una nueva instancia del modelo Teacher.

               ac.set("person_id", p.getID());
               ac.set("degree", degree);
               ac.set("email", email);
               ac.saveIt();

               Base.commitTransaction();               

               res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
               // Redirige al formulario de creación con un mensaje de éxito.
               String successMsg = URLEncoder.encode("Profesor "+firstname+" "+lastname+" registrado correctamente.",StandardCharsets.UTF_8);
               res.redirect("/teacher/create?message= " + successMsg);
               return ""; // Retorna una cadena vacía.


           } catch (Exception e) {
               // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
               // se captura aquí y se redirige con un mensaje de error.
               Base.rollbackTransaction(); // Si falla algo deshace
               e.printStackTrace(); // Imprime el stack trace para depuración.
               res.status(500); // Código de estado HTTP 500 (Internal Server Error).
               String errorMsg = URLEncoder.encode("ERROR: DNI ya existente o error interno.", StandardCharsets.UTF_8);
               res.redirect("/teacher/create?error="+errorMsg);
               return ""; // Retorna una cadena vacía. // Retorna una cadena vacía.
           }
       });


        /**
         *      Asignación de profesores
         */
        get("/teacher/assign", (req, res) -> {
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
                "errorMessage", req.queryParamOrDefault("errorMessage", ""),
                "successMessage", req.queryParamOrDefault("successMessage", "")
            );
            
            return new ModelAndView(model, "teacher_assign_form.mustache");
        }, engine);

        // "API" para  buscar a los profesores en el buscador de assign
        get("/api/teachers/search", (req, res) -> {
            res.type("application/json");
            String q = req.queryParams("q");
            if (q == null || q.trim().isEmpty()) {
                return "[]"; // retornar vacío si no hay parámetro de búsqueda
            }
            String likeQuery = "%" + q.trim() + "%";
            List<Teacher> teachers = Teacher.findBySQL(
                "SELECT t.* FROM teachers t JOIN persons p ON t.person_id = p.id WHERE p.first_name LIKE ? OR p.last_name LIKE ?", 
                likeQuery, likeQuery
            ).include(Person.class);
            // poner los resultados en una lista simple para el JSON
            List<Map<String, Object>> resultList = new java.util.ArrayList<>();
            for (Teacher t : teachers) {
                resultList.add(Map.of(
                    "id", t.getId(),
                    "fullName", t.getFullNameString(),
                    "dni", t.getDNI()
                ));
            }
            return objectMapper.writeValueAsString(resultList);
        });


        post("/teacher/assign", (req, res) -> {
            String id = req.queryParams("teacher_id");
            String subjectId = req.queryParams("subject_id");

            if (id == null || subjectId == null || id.isEmpty() || subjectId.isEmpty()) {
                res.redirect("/subject/create?error=" + URLEncoder.encode("Faltan datos obligatorios", "UTF-8"));
                return "";
            }
            try {
                Teacher t = Teacher.findById(Integer.parseInt(id));
                Subject s = Subject.findById(Integer.parseInt(subjectId));
                if (t != null && s != null) {
                    t.add(s); //funciona por el Many@Many que puse en Teacher     
                    res.redirect("/teacher/assign?successMessage=" + URLEncoder.encode("Profesor asignado a la materia correctamente", "UTF-8"));
                } else {
                    res.redirect("/teacher/assign?errorMessage=" + URLEncoder.encode("Error: Profesor o Materia no encontrados", "UTF-8"));
                }

            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMsg.contains("unique") || errorMsg.contains("primary key")) {
                    res.redirect("/teacher/assign?errorMessage=" + URLEncoder.encode("Ese profesor ya está asignado a esa materia.", "UTF-8"));
                } else {
                    e.printStackTrace();
                    res.redirect("/teacher/assign?errorMessage=" + URLEncoder.encode("Error interno al asignar.", "UTF-8"));
                }
            }
            return "";
        });


        

        /**
         *      Baja de profesores
         */
        get("/teacher/delete", (req, res) -> {
            String query = req.queryParams("q");
            List<Teacher> teachers;
            int offset = 20;
            if (query != null && !query.trim().isEmpty()) {
                teachers = Teacher.findBySQL(
                    "SELECT t.* FROM teachers t " +
                    "JOIN persons p ON t.person_id = p.id " +
                    "WHERE p.first_name LIKE ? OR p.last_name LIKE ? OR CAST(p.dni AS TEXT) LIKE ?",
                    "%" + query.trim() + "%", 
                    "%" + query.trim() + "%", 
                    "%" + query.trim() + "% LIMIT "+offset+""
                ).include(Person.class);
            } else {
                teachers = Teacher.findAll().include(Person.class);
            }

            Map<String, Object> model = Map.of(
                "tituloPagina", "Baja de docentes",
                "teachers", teachers,
                "query", (query != null)? query : "",
                "offset", offset,
                "successMessage", req.queryParamOrDefault("message", ""),
                "errorMessage", req.queryParamOrDefault("error", "")
            );

            return new ModelAndView(model, "teacher_delete.mustache");
        }, engine);

        post("/teacher/delete", (req, res) -> {
            String teacherIdStr = req.queryParams("teacher_id");
            if (teacherIdStr == null || teacherIdStr.isEmpty()) {
                res.redirect("/teacher/delete?error=" + URLEncoder.encode("ID de docente no proporcionado.", StandardCharsets.UTF_8));
                return "";
            }
            try {
                Base.openTransaction();
                Teacher t = Teacher.findById(Integer.parseInt(teacherIdStr));
                if (t != null) {
                    // asi que existis todavia... no te vas a salvar de la eliminacion eh
                    t.delete();
                    Base.commitTransaction();
                    
                    String successMsg = URLEncoder.encode("Docente eliminado correctamente del sistema.", StandardCharsets.UTF_8);
                    res.redirect("/teacher/delete?message=" + successMsg);
                } else {
                    Base.rollbackTransaction();
                    res.redirect("/teacher/delete?error=" + URLEncoder.encode("El docente no existe.", StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/teacher/delete?error=" + URLEncoder.encode("Error interno al eliminar el docente.", StandardCharsets.UTF_8));
            }
            return "";
        });



    }


}
