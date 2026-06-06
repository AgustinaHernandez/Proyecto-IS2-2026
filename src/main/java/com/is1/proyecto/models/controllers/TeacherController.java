package com.is1.proyecto.models.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.FinalSheet;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Status;
import com.is1.proyecto.models.Student;
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
                res.redirect("/teacher/create?error=" + URLEncoder.encode("El DNI y/o NrodeLegajo debe ser un número válido.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Base.openTransaction();
                
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
                t.set("file_code", fileCode);
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
                res.redirect("/teacher/assign?error=" + URLEncoder.encode("Faltan datos obligatorios", "UTF-8"));
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

        get("/teacher/select-subject", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            //Comprobar que el teacher está logueado y obtener su user
            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/");
                return null;
            }

            try {
                //Buscarlo a partir de su user
                User user = User.findFirst("name = ?", currentUsername);
                if (user != null) {
                    Teacher teacher = Teacher.findFirst("person_id = ?", user.get("person_id"));
                    if (teacher != null) {
                        List<Subject> teacherSubjects = Subject.findBySQL(
                            "SELECT s.* FROM subjects s INNER JOIN teaches t ON s.id = t.subject_id WHERE t.teacher_id = ?", 
                            teacher.getId()
                        );
                        model.put("subjects", teacherSubjects);
                        model.put("hasSubjects", !teacherSubjects.isEmpty());
                    }
                }

                String error = req.queryParams("error");
                if (error != null && !error.isEmpty()) {
                    model.put("errorMessage", error);
                }

                return new ModelAndView(model, "teacher_subject_selector.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                model.put("errorMessage", "Ocurrió un error al cargar tus materias.");
                return new ModelAndView(model, "teacher_subject_selector.mustache");
            }
        }, engine);

        get("/teacher/subject-students", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            //Comprobar que seleccionó una materia
            String subjectIdStr = req.queryParams("subject_id");
            if (subjectIdStr == null || subjectIdStr.trim().isEmpty()) {
                res.redirect("/teacher/select-subjects?error=" + URLEncoder.encode("Seleccioná una materia primero.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                Integer subjectId = Integer.parseInt(subjectIdStr);
                
                Subject subject = Subject.findById(subjectId);
                if (subject != null) {
                    model.put("subjectName", subject.getString("name"));
                }
                
                model.put("subjectId", subjectIdStr);

                String query = req.queryParams("q");
                List<Student> students;
                
                if (query != null && !query.isEmpty()) {
                    model.put("query", query);
                    String search = "%" + query.trim() + "%";
                    
                    //Une students -> statuses -> grade_sheets para filtrar por materia
                    //Se usa DISTINCT por si un alumno recursó y aparece en más de una planilla
                    String sql = "SELECT DISTINCT s.* FROM students s " +
                                "INNER JOIN persons p ON s.person_id = p.id " +
                                "INNER JOIN statuses st ON s.id = st.student_id " +
                                "INNER JOIN grade_sheets gs ON st.grade_sheet_id = gs.id " +
                                "WHERE gs.subject_id = ? " +
                                "AND (p.first_name LIKE ? OR p.last_name LIKE ? OR CAST(p.dni AS TEXT) LIKE ?)";
                    
                    students = Student.findBySQL(sql, subjectId, search, search, search);
                } else {
                    //trae todos los alumnos que estén en alguna gradesheet de la materia
                    String sql = "SELECT DISTINCT s.* FROM students s " +
                                "INNER JOIN statuses st ON s.id = st.student_id " +
                                "INNER JOIN grade_sheets gs ON st.grade_sheet_id = gs.id " +
                                "WHERE gs.subject_id = ?";
                                
                    students = Student.findBySQL(sql, subjectId);
                }

                List<Map<String, Object>> listaDeAlumnosArmada = new ArrayList<>();
                
                for (Student student : students) {
                    Person person = Person.findById(student.get("person_id"));
                    if (person != null) {
                        Map<String, Object> alumnoMap = new HashMap<>();
                        
                        alumnoMap.put("getDNI", person.get("dni"));
                        alumnoMap.put("getFullNameString", person.getString("last_name") + ", " + person.getString("first_name"));
                        alumnoMap.put("getEmail", person.getString("email") != null ? person.getString("email") : "Sin email");
                        
                        String condicion = "Desconocido";
                        
                        //Se busca el estado más reciente del alumno en la materia
                        //Después ordenamos por el año de la gradesheet de forma descendente y tomamos el primero
                        String statusSql = "SELECT st.* FROM statuses st " +
                                        "INNER JOIN grade_sheets gs ON st.grade_sheet_id = gs.id " +
                                        "WHERE gs.subject_id = ? AND st.student_id = ? " +
                                        "ORDER BY gs.year DESC";
                                        
                        List<Status> historialEstados = Status.findBySQL(statusSql, subjectId, student.getId());
                        
                        if (!historialEstados.isEmpty()) {
                            Status estadoMasReciente = historialEstados.get(0);
                            condicion = estadoMasReciente.getString("final_condition") != null ? 
                                        estadoMasReciente.getString("final_condition") : 
                                        estadoMasReciente.getString("initial_condition");
                        }
                        
                        alumnoMap.put("getCondition", condicion);
                        listaDeAlumnosArmada.add(alumnoMap);
                    }
                }

                model.put("students", listaDeAlumnosArmada);

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "teacher_students.mustache");

            } catch (NumberFormatException e) {
                res.redirect("/teacher/select-subjects?error=" + URLEncoder.encode("Materia inválida.", StandardCharsets.UTF_8));
                return null;
            }
        }, engine);

        get("/teacher/grade-final", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String finalSheetIdStr = req.queryParams("final_sheet_id");

            if (finalSheetIdStr == null || finalSheetIdStr.trim().isEmpty()) {
                res.redirect("/teacher/select-final?error=" + URLEncoder.encode("Debés seleccionar una mesa de examen.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                Integer finalSheetId = Integer.parseInt(finalSheetIdStr);
                FinalSheet finalSheet = FinalSheet.findById(finalSheetId);
                Subject subject = Subject.findById(finalSheet.get("subject_id"));

                LocalDate examDate = LocalDate.parse(finalSheet.getString("year"));
                LocalDate hoy = LocalDate.now();
                LocalDate deadline = examDate.plusDays(15); //Tiene hasta 15 días después

                if (hoy.isBefore(examDate)) {
                    res.redirect("/teacher/select-final?error=" + URLEncoder.encode("Aún no se puede calificar esta mesa.", StandardCharsets.UTF_8));
                    return null;
                }
                if (hoy.isAfter(deadline)) {
                    res.redirect("/teacher/select-final?error=" + URLEncoder.encode("El plazo de 15 días para cargar las notas venció.", StandardCharsets.UTF_8));
                    return null;
                }

                //Calcular cuántos días le quedan
                long daysLeft = ChronoUnit.DAYS.between(hoy, deadline);
                model.put("daysLeft", daysLeft);

                //Activar las alertas
                if (daysLeft == 0) {
                    model.put("lastDayWarning", true);
                } else {
                    model.put("daysWarning", true);
                }
                
                model.put("subjectName", subject.getString("name"));
                model.put("callName", finalSheet.getString("call"));
                model.put("year", finalSheet.getDate("year"));
                model.put("finalSheetId", finalSheetId);

                //Traer a todos los inscriptos
                List<Student> students = Student.findBySQL(
                    "SELECT s.* FROM students s INNER JOIN final_grades fg ON s.id = fg.student_id WHERE fg.final_sheet_id = ?", finalSheetId
                );

                List<Map<String, Object>> listadoAlumnos = new ArrayList<>();
                
                for (Student s : students) {
                    Person p = Person.findById(s.get("person_id"));
                    Map<String, Object> map = new HashMap<>();
                    map.put("studentId", s.getId());
                    map.put("getDNI", p.get("dni"));
                    map.put("getFullNameString", p.getString("last_name") + ", " + p.getString("first_name"));
                    
                    //Consultar si el alumno ya tiene una nota en la base de datos
                    Object notaObj = Base.firstCell("SELECT grade FROM final_grades WHERE final_sheet_id = ? AND student_id = ?", finalSheetId, s.getId());
                    
                    //Armar las opciones del select dinámicamente desde Java
                    List<Map<String, Object>> options = new ArrayList<>();
                    
                    //Opción default (Ausente o sin cargar), es con valor vacío
                    options.add(Map.of("value", "", "label", "Ausente / Sin Nota", "selected", notaObj == null));
                    
                    //Opciones del 1 al 10
                    for(int i = 1; i <= 10; i++) {
                        boolean isSelected = (notaObj != null && ((Number)notaObj).intValue() == i);
                        options.add(Map.of("value", String.valueOf(i), "label", String.valueOf(i), "selected", isSelected));
                    }
                    
                    map.put("gradeOptions", options);
                    listadoAlumnos.add(map);
                }

                model.put("students", listadoAlumnos);
                model.put("hasStudents", !listadoAlumnos.isEmpty());

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "teacher_grade_final.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/teacher/select-final?error=" + URLEncoder.encode("Error al cargar la planilla.", StandardCharsets.UTF_8));
                return null;
            }
        }, engine);

        post("/teacher/grade-final", (req, res) -> {
            String finalSheetIdStr = req.queryParams("final_sheet_id");
            
            try {
                FinalSheet fs = FinalSheet.findById(finalSheetIdStr);
                LocalDate examDate = LocalDate.parse(fs.getString("year"));
                LocalDate hoy = LocalDate.now();
                LocalDate deadline = examDate.plusDays(15);

                // Candado
                if (hoy.isAfter(deadline)) {
                    res.redirect("/teacher/select-final?error=" + URLEncoder.encode("El plazo para guardar notas venció. Contacte a administración.", StandardCharsets.UTF_8));
                    return "";
                }
                Base.openTransaction();
                
                for (String paramName : req.queryParams()) {
                    if (paramName.startsWith("grade_")) {
                        String studentIdStr = paramName.substring(6); //Cortar "grade_" para quedarnos con el ID
                        String gradeStr = req.queryParams(paramName);

                        if (gradeStr != null && !gradeStr.trim().isEmpty()) {
                            //Si el teacher puso un número del 1 al 10
                            Integer grade = Integer.parseInt(gradeStr);
                            Base.exec("UPDATE final_grades SET grade = ? WHERE final_sheet_id = ? AND student_id = ?", 
                                    grade, Integer.parseInt(finalSheetIdStr), Integer.parseInt(studentIdStr));
                        } else {
                            //Si el teacher puso "Ausente / Sin Nota"
                            Base.exec("UPDATE final_grades SET grade = NULL WHERE final_sheet_id = ? AND student_id = ?", 
                                    Integer.parseInt(finalSheetIdStr), Integer.parseInt(studentIdStr));
                        }
                    }
                }
                
                Base.commitTransaction();
                res.redirect("/teacher/grade-final?final_sheet_id=" + finalSheetIdStr + "&success=" + URLEncoder.encode("¡Planilla guardada correctamente!", StandardCharsets.UTF_8));
                
            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/teacher/grade-final?final_sheet_id=" + finalSheetIdStr + "&error=" + URLEncoder.encode("Error al procesar las notas.", StandardCharsets.UTF_8));
            }
            return "";
        });

        get("/teacher/select-final", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String currentUsername = req.session().attribute("currentUserUsername");
            
            if (currentUsername == null) {
                res.redirect("/");
                return null;
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                Teacher teacher = Teacher.findFirst("person_id = ?", user.get("person_id"));

                if (teacher == null) {
                    res.redirect("/dashboard?error=" + URLEncoder.encode("Tu usuario no está registrado como docente.", StandardCharsets.UTF_8));
                    return null;
                }

                String hoy = LocalDate.now().toString();
                String limitePasado = LocalDate.now().minusDays(7).toString();

                // SQL: "WHERE fs.year <= ?" para que no pueda calificar un examen del futuro
                String sql = "SELECT fs.id as final_sheet_id, fs.call, fs.year, s.name as subject_name " +
                    "FROM final_sheets fs " +
                    "INNER JOIN subjects s ON fs.subject_id = s.id " +
                    "INNER JOIN teaches t ON s.id = t.subject_id " +
                    "WHERE t.teacher_id = ? AND fs.year <= ? AND fs.year >= ? " +
                    "ORDER BY fs.year DESC, fs.call ASC";
                
                List<Map> mesasDelProfe = Base.findAll(sql, teacher.getId(), hoy, limitePasado);
                
                model.put("finalSheets", mesasDelProfe);
                model.put("hasFinalSheets", !mesasDelProfe.isEmpty());

                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "teacher_select_final.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                model.put("errorMessage", "Error interno al cargar las mesas de examen.");
                return new ModelAndView(model, "teacher_select_final.mustache");
            }
        }, engine);
    }


}
