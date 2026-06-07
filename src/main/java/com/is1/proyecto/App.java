package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.

// Importaciones de Spark para renderizado de plantillas
import spark.ModelAndView; // Representa un modelo de datos y el nombre de la vista a renderizar.
import spark.template.mustache.MustacheTemplateEngine; // Motor de plantillas Mustache para Spark.

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
// Importaciones estándar de Java
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.List;
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.

// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.controllers.*;
import com.is1.proyecto.models.*;


/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {
    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 4567).
            
        // Obtener la instancia única del singleton de configuración de la base de datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // --- Filtro 'before' para gestionar la conexión a la base de datos ---
        // Este filtro se ejecuta antes de cada solicitud HTTP.
        before("/*", (req, res) -> {
            try {
                if (!Base.hasConnection()) {
                    Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                }
            } catch (Exception e) {
                System.err.println("Error crítico al abrir la DB: " + e.getMessage());
                halt(500, "Error interno de base de datos.");
            }
        });
        // Filtro de seguridad que restringe el acceso solo al admin
        // Las rutas protegidas revisan el atributo isAdmin e isStudent de la sesion
        before("/teacher/create", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/new", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/assign", (req, res) -> checkAdminAccess(req, res));
        before("/subject/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/new", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/unassign", (req, res) -> checkAdminAccess(req, res));
        before("/student/create", (req, res) -> checkAdminAccess(req, res));

        // --- Filtro 'after-after' para cerrar la conexión a la base de datos pase lo que pase---
        afterAfter("/*", (req, res) -> {
            if (Base.hasConnection()) {
                Base.close();
            }
        });

        // --- Filtro 'after' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        after((req, res) -> {
            try {
                // Cierra la conexión a la base de datos para liberar recursos.
                Base.close();
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });
        

        // --- Rutas GET para renderizar formularios y páginas HTML ---


        // Rutas refactorizadas
        
        // Dashboard
        get("/dashboard", DashboardController::renderDashboard, new MustacheTemplateEngine());
        
        // Settings
        get("/settings", ProfileController::renderSettings, new MustacheTemplateEngine());

        // Alta de carrera
        get("/career/create", CareerController::renderCreateForm, new MustacheTemplateEngine());
        post("/career/new", CareerController::handleCreateCareer);

 
        // Alta de plan
        get("/plan/new", PlanController::renderCreateForm, new MustacheTemplateEngine());
        post("/plan/new", PlanController::handleCreatePlan);

        // Modificacion de plan
        get("/plan/update", PlanController::renderUpdateForm, new MustacheTemplateEngine());
        post("/plan/update", PlanController::handleUpdatePlan);

        // Consulta de plan
        get("/plans", PlanController::renderQueryForm, new MustacheTemplateEngine());
        post("/plans", PlanController::handleQueryPlan, new MustacheTemplateEngine());

        // Perfil
        get("/profile", ProfileController::renderProfile, new MustacheTemplateEngine());
        // Cambiar email (y verificación con código enviado por mail)
        get("/profile/verify-email", ProfileController::renderVerifyEmail, new MustacheTemplateEngine());
        // POST: Pedir el cambio de correo y enviar código
        post("/profile/update-email", ProfileController::handleUpdateEmailRequest);
        // POST: Confirmar el código y guardar
        post("/profile/verify-email", ProfileController::handleVerifyEmailCode);


        // GET: Recuperar contraseña (mandar código)
        get("/recover-password", AuthController::renderRecoverPassword, new MustacheTemplateEngine());
        // POST: Recuperar contraseña (mandar código)
        post("/recover-password", AuthController::handleRecoverPasswordRequest);
        // GET: Recuperar contraseña (insertar código y contraseña vieja)
        get("/reset-password", AuthController::renderResetPassword, new MustacheTemplateEngine());
        // POST: Recuperar contraseña
        post("/reset-password", AuthController::handleResetPasswordRequest);


        // GET: Cambiar contraseña (estando logeado)
        get("/change-password", AuthController::renderChangePassword, new MustacheTemplateEngine());
        // POST: Cambiar contraseña (estando logeado)
        post("/change-password", AuthController::handleChangePassword);


        //GET: Alta de teacher
        get("/teacher/create", TeacherController::renderTeacherCreation, new MustacheTemplateEngine());
        //POST: Alta de teacher
        post("/teacher/create", TeacherController::handleTeacherCreation);


        //GET: Asignación de teacher a materia
        get("/teacher/assign", TeacherController::renderTeacherAssign, new MustacheTemplateEngine());
        //GET "API": Buscar profesores
        get("/api/teachers/search", TeacherController::handleSearchTeachersAPI);
        //POST: Alta de teacher
        post("/teacher/assign", TeacherController::handleTeacherAssignation);


        //GET: Alta de estudiantes
        get("/student/create", StudentController::renderCreationForm, new MustacheTemplateEngine());
        //POST: Alta de estudiantes
        post("/student/create", StudentController::handleStudentCreation);

        //GET: Baja de estudiantes
        get("/student/delete", StudentController::renderDeleteForm, new MustacheTemplateEngine());
        //POST: Baja de estudiantes
        post("/student/delete", StudentController::handleStudentDelete);
        
        
        //GET: Baja de Profesores
        get("/teacher/delete", TeacherController::renderDeleteForm, new MustacheTemplateEngine());
        //POST: Baja de Profesores
        post("/teacher/delete", TeacherController::handleDeleteTeacher);


        //GET: Alta de materia
        get("/subject/create", SubjectController::renderSubjectCreation, new MustacheTemplateEngine());
        //POST: Alta de materia
        post("/subject/create", SubjectController::handleSubjectCreation);     


        // GET: Muestra el formulario de inicio de sesión (login).
        get("/", AuthController::renderLoginForm, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        //GET: /logout
        get("/logout", AuthController::handleLogout);

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/", AuthController::handleLogin, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta POST

        post("/set-role", ProfileController::handleSetRole);
        
        get("/enrollment", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            Object userIdAttr = req.session().attribute("userId");
            if (userIdAttr == null) {
                req.session().attribute("error", "Acceso no autorizado.");
                res.redirect("/"); 
                return null;
            }
            
            String activeRole = req.session().attribute("activeRole");
            Boolean isStudent = req.session().attribute("isStudent");
            
            if (!"STUDENT".equals(activeRole) || !Boolean.TRUE.equals(isStudent)) {
                req.session().attribute("error", "Acceso denegado: Solo los usuarios con el rol de Estudiante activo pueden inscribirse.");
                res.redirect("/dashboard"); // Te redirige a tu pantalla de bienvenida
                return null;
            }
            
            User user = User.findById(userIdAttr);
            if (user == null) {
                res.redirect("/");
                return null;
            }
            
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));
            if (student == null) {
                req.session().attribute("error", "Error interno: No se encontró un perfil de estudiante para tu cuenta.");
                res.redirect("/dashboard");
                return null;
            }

            List<Subject> availableSubjects = student.getAvailableSubjectsToEnroll();
            model.put("materias", availableSubjects);
            model.put("hasMaterias", !availableSubjects.isEmpty());

            model.put("username", req.session().attribute("currentUserUsername"));
            model.put("activeRole", activeRole);
            model.put("isActiveStudent", true);

            if (req.session().attribute("error") != null) {
                model.put("error", req.session().attribute("error"));
                req.session().removeAttribute("error");
            }
            if (req.session().attribute("success") != null) {
                model.put("success", req.session().attribute("success"));
                req.session().removeAttribute("success");
            }

            return new ModelAndView(model, "enrollment.mustache");
        }, new MustacheTemplateEngine());

        
        get("/teacher/unassign", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            // Control de Seguridad (Solo ADMIN)
            Boolean isAdmin = req.session().attribute("isAdmin");
            if (isAdmin == null || !isAdmin) {
                res.redirect("/dashboard?error=Acceso restringido.");
                return null;
            }

            // Traemos las materias de la base de datos para rellenar el select del formulario
            List<Subject> subjects = Subject.findAll();
            model.put("subjects", subjects);


            if (req.session().attribute("errorMessage") != null) {
                model.put("errorMessage", req.session().attribute("errorMessage"));
                req.session().removeAttribute("errorMessage");
            }
            if (req.session().attribute("successMessage") != null) {
                model.put("successMessage", req.session().attribute("successMessage"));
                req.session().removeAttribute("successMessage");
            }

            return new ModelAndView(model, "teacher_unassign.mustache");
        }, new MustacheTemplateEngine());

        post("/enrollment", (req, res) -> {
            Object userIdAttr = req.session().attribute("userId");
            String activeRole = req.session().attribute("activeRole");

            if (userIdAttr == null || !"STUDENT".equals(activeRole)) {
                res.redirect("/");
                return null;
            }

            String subjectIdParam = req.queryParams("subject_id");
            if (subjectIdParam == null || subjectIdParam.isEmpty()) {
                req.session().attribute("error", "Selección de materia inválida.");
                res.redirect("/enrollment");
                return null;
            }

            User user = User.findById(userIdAttr);
            Student student = (user != null) ? Student.findFirst("person_id = ?", user.get("person_id")) : null;
            Subject subject = Subject.findById(subjectIdParam);

            if (student == null || subject == null) {
                req.session().attribute("error", "Error de consistencia: Alumno o materia no encontrados.");
                res.redirect("/enrollment");
                return null;
            }

            List<String> errors = student.validateEnrollment(subject);
            if (!errors.isEmpty()) {
                req.session().attribute("error", "Inscripción rechazada: " + String.join(" ", errors));
                res.redirect("/enrollment");
                return null;
            }

            try {
                Base.openTransaction();

                // Buscamos el acta (grade_sheet) de cursado de la materia para el ciclo lectivo 2026
                String gsSql = "SELECT id FROM grade_sheets WHERE subject_id = ? AND year = ? LIMIT 1";
                Object gradeSheetId = Base.firstCell(gsSql, subject.getId(), 2026);

                Base.exec("INSERT INTO statuses (grade_sheet_id, student_id, initial_condition, final_condition) VALUES (?, ?, ?, ?)", 
                          gradeSheetId, student.getId(), "INSCRIPTO", "INSCRIPTO");

                Base.commitTransaction();
                req.session().attribute("success", "¡Te has inscripto con éxito a " + subject.getString("name") + "!");
                
            } catch (Exception e) {
                Base.rollbackTransaction();
                req.session().attribute("error", "Ocurrió un error inesperado en el servidor al procesar el alta.");
                e.printStackTrace();
            }

            res.redirect("/enrollment");
            return null;
        });

        post("/teacher/unassign", (req, res) -> {
            
            // Verificación de Seguridad (Solo ADMIN)
            Boolean isAdmin = req.session().attribute("isAdmin");
            if (isAdmin == null || !isAdmin) {
                res.redirect("/dashboard");
                return null;
            }

            String teacherIdStr = req.queryParams("teacher_id");
            String subjectIdStr = req.queryParams("subject_id");

            if (teacherIdStr == null || subjectIdStr == null || teacherIdStr.isEmpty() || subjectIdStr.isEmpty()) {
                req.session().attribute("errorMessage", "Todos los campos son requeridos.");
                res.redirect("/teacher/unassign");
                return null;
            }

            try {
                Integer teacherId = Integer.parseInt(teacherIdStr);
                Integer subjectId = Integer.parseInt(subjectIdStr);

                Subject subject = Subject.findById(subjectId);
                if (subject == null) {
                    req.session().attribute("errorMessage", "La materia seleccionada no existe.");
                    res.redirect("/teacher/unassign");
                    return null;
                }

                // Evitar desasignar al profesor responsable de la materia
                if (subject.getInteger("responsible_id").equals(teacherId)) {
                    req.session().attribute("errorMessage", "No puedes desasignar a este profesor porque figura como el Responsable obligatorio de la materia. Asigna a otro responsable antes de removerlo.");
                    res.redirect("/teacher/unassign");
                    return null;
                }

                // PROCESO DE DESASIGNACIÓN
                Base.openTransaction();
                
                int rowsDeleted = Base.exec("DELETE FROM teaches WHERE teacher_id = ? AND subject_id = ?", teacherId, subjectId);
                
                if (rowsDeleted > 0) {
                    Base.commitTransaction();
                    req.session().attribute("successMessage", "El profesor ha sido desasignado exitosamente de la asignatura.");
                } else {
                    Base.rollbackTransaction();
                    req.session().attribute("errorMessage", "El profesor ingresado no pertenece al equipo docente asignado a esta materia.");
                }

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                req.session().attribute("errorMessage", "Ocurrió un error inesperado al procesar la desasignación.");
            }

            res.redirect("/teacher/unassign");
            return null;
        });



    } // Fin del método main

    /**
     * Filtro de verificación de acceso administrativo.
     * Solo permite el acceso si el usuario tiene el flag 'isAdmin' en true en la sesión.
     */
    private static void checkAdminAccess(spark.Request req, spark.Response res) {
        Boolean isAdmin = (Boolean) req.session().attribute("isAdmin");
        Boolean loggedIn = (Boolean) req.session().attribute("loggedIn");
        String currentUsername = req.session().attribute("currentUserUsername");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=" + URLEncoder.encode("Acceso no autorizado.", StandardCharsets.UTF_8));
            halt(); 
            return;
        }

        if (isAdmin == null || !isAdmin) {
            System.out.println("DEBUG: Acceso a ruta de administrador denegado al usuario: " + currentUsername);
            res.redirect("/dashboard?error=" + URLEncoder.encode("Acceso denegado. Solo el administrador puede acceder.", StandardCharsets.UTF_8));
            halt(); 
        }
    }

} // Fin de la clase App
