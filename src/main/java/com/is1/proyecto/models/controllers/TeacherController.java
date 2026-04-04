package com.is1.proyecto.models.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User;
import com.is1.proyecto.utils.EmailSender;
import com.is1.proyecto.utils.PasswordGenerator;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;
import static spark.Spark.*;
import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt; 

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

            Integer dni;
            try {
                dni = Integer.parseInt(dniStr);
                if (dni <= 0) throw new IllegalArgumentException();
            } catch (Exception e) {
                res.redirect("/teacher/create?error=" + URLEncoder.encode("El DNI debe ser un número válido.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Base.openTransaction();
                
                // Reaplicamos la lógica de alta de estudiante acá
                Person p = Person.findFirst("dni = ?", dni);
                if (p == null) {
                    p = new Person();
                    p.set("first_name", firstname);
                    p.set("last_name", lastname);
                    p.set("dni", dni);
                    p.set("email", email);
                    p.saveIt();
                } else {
                    Teacher existing = Teacher.findFirst("person_id = ?", p.getId());
                    if (existing != null) {
                        Base.rollbackTransaction();
                        res.redirect("/teacher/create?error=" + URLEncoder.encode("Esta persona ya está registrada como profesor.", StandardCharsets.UTF_8));
                        return "";
                    }
                    p.set("email", email); // Actualizar mail por las dudas
                    p.saveIt();
                }

                Teacher t = new Teacher();
                t.set("person_id", p.getId());
                t.set("degree", degree);
                t.saveIt();

                // Solo si no tiene un usuario previamente
                User u = User.findFirst("person_id = ?", p.getId());
                String randomPassword = PasswordGenerator.generateSecurePassword(12);
                boolean isNewUser = (u == null);

                if (isNewUser) {
                    u = new User();
                    String hashedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt());
                    u.set("name", dniStr);
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
                        String asunto;
                        String cuerpoHtml;
                        if (finalIsNewUser) {
                            asunto = "Bienvenido al cuerpo docente - Credenciales de acceso";
                            cuerpoHtml = "<div style='font-family: Arial; max-width: 600px; border: 1px solid #eee; border-radius: 10px; overflow: hidden;'>" +
                                "<div style='background: #2563eb; padding: 20px; text-align: center; color: white;'><h2>¡Bienvenido, Profe!</h2></div>" +
                                "<div style='padding: 30px;'>" +
                                "<p>Hola <b>" + firstname + "</b>, tu cuenta docente ha sido creada.</p>" +
                                "<div style='background: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                                "<b>Usuario (DNI):</b> " + dniStr + "<br><b>Contraseña:</b> <code style='background: #ddd; padding: 2px 5px;'>" + finalPassword + "</code>" +
                                "</div>" +
                                "<p style='color: #666; font-size: 13px;'>Por seguridad, cambia tu clave al ingresar.</p>" +
                                "</div></div>";
                        } else {
                            asunto = "Nuevo perfil habilitado: Profesor";
                            cuerpoHtml = "<div style='font-family: Arial; max-width: 600px; border: 1px solid #eee; border-radius: 10px; overflow: hidden;'>" +
                                "<div style='background: #10b981; padding: 20px; text-align: center; color: white;'><h2>Perfil Docente Habilitado</h2></div>" +
                                "<div style='padding: 30px;'>" +
                                "<p>Hola <b>" + firstname + "</b>, ahora tienes acceso al sistema como <b>Profesor</b>.</p>" +
                                "<p>Usa tus credenciales de siempre (DNI y tu clave actual). Podrás alternar roles desde el menú superior del Dashboard.</p>" +
                                "</div></div>";
                        }
                        EmailSender.sendMail(finalEmail, asunto, cuerpoHtml);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                res.redirect("/teacher/create?message=" + URLEncoder.encode("Profesor " + firstname + " " + lastname + " registrado correctamente.", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/teacher/create?error=" + URLEncoder.encode("Error interno al procesar el alta.", StandardCharsets.UTF_8));
                return "";
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
