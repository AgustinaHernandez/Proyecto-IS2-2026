package com.is1.proyecto.models.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

public class TeacherControllerOld {
    
    public static String registrarRutas(MustacheTemplateEngine engine, ObjectMapper objectMapper) {
        /**
         *      Modificacion de Datos de profesores
         */
        /* get("/teacher/modify", (req, res) -> {
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
                "tituloPagina", "Modificar Datos Docentes",
                "teachers", teachers,
                "query", (query != null)? query : "",
                "offset", offset,
                "successMessage", req.queryParamOrDefault("message", ""),
                "errorMessage", req.queryParamOrDefault("error", "")
            );

            return new ModelAndView(model, "teacher_modify.mustache");
        }, engine); */

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
        
        post("/teacher/modify", (req, res) -> {
            String teacherIdStr = req.queryParams("teacher_id");
        
            Map<String, Object> paqueteMoustache = new HashMap<>();

            List<Teacher> listaProfesores = Teacher.findBySQL("SELECT * FROM teachers WHERE id = ?", teacherIdStr);
    
            if (!listaProfesores.isEmpty()) {
                Teacher profe = listaProfesores.get(0);
        
                paqueteMoustache.put("teacher_id", teacherIdStr);
                paqueteMoustache.put("degree", profe.get("degree"));
                paqueteMoustache.put("fcode", profe.get("id"));
        
                Object idDeLaPersona = profe.get("person_id");
                paqueteMoustache.put("idPerson", idDeLaPersona);

                List<Person> listaPersonas = Person.findBySQL("SELECT * FROM persons WHERE id = ?", idDeLaPersona);
        
                if (!listaPersonas.isEmpty()) {
                    Person pers = listaPersonas.get(0);
            
                    paqueteMoustache.put("firstname", pers.get("first_name"));
                    paqueteMoustache.put("lastname", pers.get("last_name"));
                    paqueteMoustache.put("dni", pers.get("dni"));                    
                    paqueteMoustache.put("email", pers.get("email"));
                }
            }

            // 4. Respondemos al POST dibujando la SEGUNDA página (el formulario de edición)
            // Moustache va a recibir el paquete y va a rellenar los campos
            return new ModelAndView(paqueteMoustache, "teacher_update.mustache");
        }, new MustacheTemplateEngine());

        post("/teacher/update", (req, res) -> {
            // Capturamos los datos del formulario usando los "name" del HTML
            String idProfesor   = req.queryParams("teacher_id");
            String first_name   = req.queryParams("firstname");
            String last_name    = req.queryParams("lastname");
            String e_mail        = req.queryParams("email");
            String grade        = req.queryParams("degree"); // Captura el <select>
            
            // Nota: DNI y Legajo son "readonly" en tu HTML, pero conviene tenerlos 
            // en caso de que necesites validarlos o rellenar de nuevo el modelo si algo falla.
            String dni          = req.queryParams("dni");
            String legajo       = req.queryParams("file_code");

            try {
                // Iniciamos la transacción con la Base de Datos
                 Base.openTransaction();
        
                // Buscamos al docente por su ID
                Teacher t = Teacher.findById(Integer.parseInt(idProfesor));

                if (t != null) {
                    // Modificamos el campo propio de la tabla 'teachers'
                     t.set("degree", grade);
                     t.saveIt(); // Guardamos el cambio del grado académico                    

                    // Obtenemos la persona vinculada a este profesor
                    // Nota: Dependiendo de cómo definiste la relación en ActiveJDBC, 
                    // se puede obtener comúnmente con t.parent(Person.class) o t.get(Person.class)
                    Person p = t.parent(Person.class); 
            
                    if (p != null) {
                        // 5. Modificamos los campos de la tabla 'persons'
                        p.set("first_name", first_name);
                        p.set("last_name", last_name);
                        p.set("email", e_mail);
                        //p.set("file_code", req.queryParams("file_code")); // Por si acaso
                        p.saveIt(); // Guardamos los cambios en la tabla persons
                    }
                    
                    // Guardamos los cambios y confirmamos la transacción
                    Base.commitTransaction();
            
                    // Redirección con mensaje de éxito (puedes mandarlo al dashboard o al mismo formulario)
                    String successMessage = URLEncoder.encode("Docente modificado correctamente en el sistema.", StandardCharsets.UTF_8);
                    res.redirect("/dashboard?successMessage=" + successMessage);
                    return null;
            
                } else {
                    Base.rollbackTransaction();
                    String errorMessage = URLEncoder.encode("El docente no existe.", StandardCharsets.UTF_8);
                    res.redirect("/dashboard?errorMessage=" + errorMessage);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Base.rollbackTransaction();                

                Map<String, Object> model = new HashMap<>();
                model.put("errorMessage", "Error al actualizar en la base de datos: " + e.getMessage());
                
                // Reinyectamos los datos para que el usuario no tenga que escribirlos de nuevo
                model.put("firstname", first_name);
                model.put("lastname", last_name);
                model.put("dni", dni);
                model.put("email", e_mail);
                model.put("file_code", legajo);
                model.put("degree", grade);
                
                return new MustacheTemplateEngine().render(
                    new ModelAndView(model, "teacher_update.mustache")
                );
            }
        });
     return null;}

}
