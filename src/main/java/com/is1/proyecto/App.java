package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.mindrot.jbcrypt.BCrypt; // Utilidad para hashear y verificar contraseñas de forma segura.

// Importaciones de Spark para renderizado de plantillas
import spark.ModelAndView; // Representa un modelo de datos y el nombre de la vista a renderizar.
import spark.template.mustache.MustacheTemplateEngine; // Motor de plantillas Mustache para Spark.

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
// Importaciones estándar de Java
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.List;
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.

// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.models.*;
import com.is1.proyecto.models.controllers.TeacherController;
import com.is1.proyecto.utils.EmailSender;
import com.is1.proyecto.utils.PasswordGenerator;


/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        before("/subject/new", (req, res) -> checkAdminAccess(req, res));
        before("/career/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/new", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/unassign", (req, res) -> checkAdminAccess(req, res));
        before("/grade-enrollments", (req, res) -> checkStudentAccess(req, res));
        before("/final-enrollments", (req, res) -> checkStudentAccess(req, res));
        before("/academic-performance", (req, res) -> checkStudentAccess(req, res));

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

        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        TeacherController.registrarRutas(engine, objectMapper);
        

        // --- Rutas GET para renderizar formularios y páginas HTML ---


        //GET: Muestra el formulario de recuperación de contraseña.
        get("/recover-password", (req, res) -> {

            Map<String, Object> model = Map.of(
                "tituloPagina", "Recuperar contraseña",
                "errorMessage", req.queryParamOrDefault("error", ""),
                "successMessage", req.queryParamOrDefault("message", "")
            );

            return new ModelAndView(model, "recover_password.mustache");
        }, new MustacheTemplateEngine());

        //GET: Muestra el formulario de ingreso de código y consecuente reseteo de contraseña.
        get("/reset-password", (req, res) -> {

            Map<String, Object> model = Map.of(
                "tituloPagina", "Ingresar código",
                "errorMessage", req.queryParamOrDefault("error", ""),
                "successMessage", req.queryParamOrDefault("message", "")
            );

            return new ModelAndView(model, "reset_password.mustache");
        }, new MustacheTemplateEngine());

        //GET: Muestra el formulario de cambio de contraseña "voluntario" (estando logeado)
        get("/change-password", (req, res) -> {
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /change-password. Redirigiendo a /.");
                res.redirect("/?error=Acceso+no+autorizado.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("tituloPagina", "Cambiar contraseña");

            String error = req.queryParams("error");
            if (error != null && !error.isEmpty()) {
                model.put("errorMessage", error);
            }

            String success = req.queryParams("success");
            if (success != null && !success.isEmpty()) {
                model.put("successMessage", success);
            }

            return new ModelAndView(model, "change_password.mustache");
        }, new MustacheTemplateEngine());


        get("/profile/verify-email", (req, res) -> {
            if (req.session().attribute("userId") == null) {
                res.redirect("/");
                return null;
            }

            // Si entra acá sin haber pedido un cambio, lo pateamos al perfil
            if (req.session().attribute("pendingEmail") == null) {
                res.redirect("/profile");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("tituloPagina", "Verificar Correo");
            model.put("pendingEmail", req.session().attribute("pendingEmail"));
            
            String error = req.queryParams("error");
            if (error != null && !error.isEmpty()) {
                model.put("errorMessage", error);
            }

            return new ModelAndView(model, "verify_email.mustache");
        }, new MustacheTemplateEngine());

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            
            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /.");
                // Redirige al login con un mensaje de error.
                res.redirect("/?error=Acceso no autorizado.");
                return null; // Importante retornar null después de una redirección.
            }
            

            //Ver qué roles tiene y cuántos son
            boolean isStudent = (boolean) req.session().attribute("isStudent");
            boolean isRegularStudent = (boolean) req.session().attribute("isRegularStudent");
            boolean isTeacher = (boolean) req.session().attribute("isTeacher");
            boolean isAdmin = (boolean) req.session().attribute("isAdmin");
            int roleCount = (isAdmin ? 1 : 0) + (isStudent ? 1 : 0) + (isTeacher ? 1:0);

            // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
            model.put("username", currentUsername);

            //Agregar rol activo
            String activeRole = (String) req.session().attribute("activeRole");
            if (activeRole == null) {
                if (isAdmin) activeRole = "ADMIN";
                else if (isTeacher) activeRole = "TEACHER";
                else if (isStudent) activeRole = "STUDENT";
                else activeRole = "NONE";
                req.session().attribute("activeRole", activeRole);
            }

            //Agregar todos los roles para poder pasarlos al desplegable donde el usuario puede cambiar de rol
            model.put("hasMultipleRoles", roleCount > 1);
            model.put("isAdmin", isAdmin);
            model.put("isTeacher", isTeacher);
            model.put("isStudent", isStudent);
            model.put("isRegularStudent", isRegularStudent);
            model.put("activeRole",activeRole);    
            model.put("isActiveAdmin", "ADMIN".equals(activeRole));
            model.put("isActiveTeacher", "TEACHER".equals(activeRole));
            model.put("isActiveStudent", "STUDENT".equals(activeRole));        
            model.put("hasNoRole", "NONE".equals(activeRole)); //Usado en renderización de materias en dashboard
            // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.
        // GET: Página de configuración
        get("/settings", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            // Verificar sesión (opcional, pero recomendado)
            if (req.session().attribute("loggedIn") == null) {
                res.redirect("/");
                return null;
            }
            model.put("tituloPagina", "Configuración");
            return new ModelAndView(model, "settings.mustache");
        }, new MustacheTemplateEngine());

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de los query params
        // si se la usa como destino de redirecciones. 
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.



        get("/profile", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Object rawUserId = req.session().attribute("userId");
            String currentUsername = req.session().attribute("currentUserUsername");
            String role = req.session().attribute("activeRole");
            Long userId = null;

            if (rawUserId != null) {
                try {
                    userId = Long.valueOf(rawUserId.toString()); 
                } catch (NumberFormatException e) {
                    System.err.println("ERROR: El userId en sesión no es un número válido. Forzando logout. " + e.getMessage());
                    res.redirect("/logout");
                    return null;
                }
            }
            
            if (userId == null) {
                res.redirect("/?error=Acceso no autorizado.");
                return null;
            }

            User currentUser = User.findById(userId);

            if (currentUser == null) {
                res.redirect("/logout");
                return null;
            }
            
            Person currentPerson = Person.findById(currentUser.get("person_id"));
            String fullName = (String) currentPerson.getLastName() + ", " + (String) currentPerson.getFirstName();

            model.put("userId", currentUser.getId());
            model.put("username", currentUsername); 
            model.put("fullName", fullName);
            model.put("dni", currentPerson.getDNI());
            model.put("email", currentPerson.getMail());
            model.put("tituloPagina", "Perfil de Usuario");

            String error = req.queryParams("error");
            if (error != null && !error.isEmpty()) {
                model.put("errorMessage", error);
            }
            String success = req.queryParams("success");
            if (success != null && !success.isEmpty()) {
                model.put("successMessage", success);
            }

            String degree = null;
            Integer file_code = null;
            if(role.equals("TEACHER")){ //Si es teacher, tiene título además de los otros datos
                List<Teacher> teacher = Teacher.find("person_id = ?", currentPerson.getID());
                degree = teacher.get(0).getDegree();       
                file_code = teacher.get(0).getFileCode();
            }
            model.put("degree", degree); //Si lo mapea como null, el formulario lo detecta y no lo muestra (ver perfil_usuario.mustache)
            model.put("file_code", file_code);
            return new ModelAndView(model, "perfil_usuario.mustache");
        }, new MustacheTemplateEngine());


        get("/subject/create", (req, res) -> {
            // select de todos los profesores con sus datos de la tabla persona
            List<Teacher> teachers = Teacher.findAll().include(Person.class);
            // buscamos los planes
            List<Plan> plans = Plan.findAll().include(Career.class); 
            // mapeo para pasarle al mustache luego
            Map<String, Object> model = Map.of(
                "tituloPagina", "Alta de materia",
                "teachers", teachers,
                "plans", plans, // agregar planes al modelo
                "errorMessage", req.queryParamOrDefault("errorMessage", ""),
                "successMessage", req.queryParamOrDefault("successMessage", "")
            );
            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        get("/career/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Carrera agregada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            model.put("tituloPagina", "Alta de carrera");

            // Renderiza la plantilla 'career_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


        get("/plan/new",(req, res) -> { 
            List<Plan> plans = Plan.findAll().include(Career.class);

            List<Career> careers = Career.findAll(); 

            Map<String, Object> model = Map.of(
                "plans", plans,
                "careers",careers,
                "tituloPagina", "Nuevo plan",
                "errorMessage", req.queryParamOrDefault("errorMessage", ""),
                "successMessage", req.queryParamOrDefault("successMessage", "")
            );
            return new ModelAndView(model, "plan_new.mustache");

        }, new MustacheTemplateEngine());


        get("/plan/update",(req, res) -> { 
            List<Plan> plans = Plan.findAll().include(Career.class);

            Map<String, Object> model = Map.of(
                "plans", plans,
                "tituloPagina", "Modificación de plan",
                "errorMessage", req.queryParamOrDefault("errorMessage", ""),
                "successMessage", req.queryParamOrDefault("successMessage", "")
            );
            return new ModelAndView(model, "plan_update.mustache");

        }, new MustacheTemplateEngine());

        get("/student/delete", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Renderiza la plantilla 'student_del.mustache' con los datos del modelo.
            return new ModelAndView(model, "student_del.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        get("/student/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Carrera agregada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Renderiza la plantilla 'career_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.
        

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

        get("/plans",(req, res) -> { 
            Map<String, Object> model = new HashMap<>();
            List<Plan> plans = Plan.findAll().include(Career.class);

            model.put("plans", plans);

            return new ModelAndView(model, "plans.mustache");

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

        get("/student/subjects", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));

            List<Enrolled_Plan> enrolled = Enrolled_Plan.where("student_id = ?", student.getId()).include(Plan.class);

            model.put("enrolled", enrolled);
            return new ModelAndView(model, "choose_subjects.mustache");
        }, new MustacheTemplateEngine());

        get("/academic-performance", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));

            List<Enrolled_Plan> enrolled = Enrolled_Plan.where("student_id = ?", student.getId()).include(Plan.class);

            model.put("enrolled", enrolled);
            return new ModelAndView(model, "enrolled_careers.mustache");
        }, new MustacheTemplateEngine());
        
        // GET: Muestra el formulario de inscripción a la carrera/plan
        get("/student/enroll-plan", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return null;
            }

            try {
                //Verificar que el usuario sea un estudiante
                User user = User.findFirst("name = ?", currentUsername);
                if (user != null) {
                    Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                    
                    if (student == null) {
                        res.redirect("/dashboard?error=" + URLEncoder.encode("Tu cuenta no está registrada como estudiante.", StandardCharsets.UTF_8));
                        return null;
                    }

                    //Traer solo los planes que estén VIGENTE o A TERMINO
                    List<Plan> availablePlans = Plan.where("state IN ('VIGENTE', 'A TERMINO')");
                    List<Map<String, Object>> planesList = new ArrayList<>();

                    for (Plan plan : availablePlans) {
                        Career career = Career.findById(plan.get("career_id"));
                        if (career != null) {
                            Map<String, Object> planMap = new HashMap<>();
                            planMap.put("planId", plan.getId());
                            String displayName = career.getString("name") + " - Plan " + plan.get("version") + " (" + plan.getString("state") + ")";
                            planMap.put("displayName", displayName);
                            
                            planesList.add(planMap);
                        }
                    }

                    model.put("plans", planesList);
                    model.put("hasPlans", !planesList.isEmpty());
                }

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "student_enroll_plan.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                model.put("errorMessage", "Error al cargar los planes de estudio.");
                return new ModelAndView(model, "student_enroll_plan.mustache");
            }
        }, engine);

        get("/final/new", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            try {
                //Traer todas las carreras para el primer desplegable
                List<Map<String, Object>> careers = Career.findAll().toMaps();
                model.put("careers", careers);
                
                //Traer las materias con su id de carrera
                //Se usa DISTINCT por si la materia está en varios planes de la misma carrera
                String sqlSubjects = "SELECT DISTINCT s.id, s.name, c.id as career_id FROM subjects s " +
                                    "INNER JOIN subject_belongs_plan sbp ON s.id = sbp.subject_id " +
                                    "INNER JOIN plans p ON sbp.plan_id = p.id " +
                                    "INNER JOIN careers c ON p.career_id = c.id " +
                                    "ORDER BY s.name ASC";
                
                List<Map> subjects = Base.findAll(sqlSubjects);
                model.put("subjects", subjects);

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

            } catch (Exception e) {
                model.put("errorMessage", "Error al cargar los datos del formulario.");
            }
            
            return new ModelAndView(model, "final_new.mustache");
        }, engine);


        get("/student/enroll-final", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return null;
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                if (user != null) {
                    Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                    
                    if (student == null) {
                        res.redirect("/dashboard?error=" + URLEncoder.encode("Tu cuenta no está registrada como estudiante.", StandardCharsets.UTF_8));
                        return null;
                    }

                    String today = LocalDate.now().toString();
                    String sql = "SELECT fs.id as sheet_id, s.name as subject_name, fs.year as exam_date, fs.call " +
                                 "FROM final_sheets fs " +
                                 "INNER JOIN subjects s ON fs.subject_id = s.id " +
                                 "WHERE fs.year >= ? " +
                                 "ORDER BY fs.year ASC";
                    
                    List<Map> availableFinals = Base.findAll(sql, today);

                    model.put("finals", availableFinals);
                    model.put("hasFinals", !availableFinals.isEmpty());
                }

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "student_enroll_final.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                model.put("errorMessage", "Error al cargar las mesas de examen.");
                return new ModelAndView(model, "student_enroll_final.mustache");
            }
        }, new MustacheTemplateEngine());


        get("/student/unenroll-final", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return null;
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                if (user != null) {
                    Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                    
                    if (student == null) {
                        res.redirect("/dashboard?error=" + URLEncoder.encode("Tu cuenta no está registrada como estudiante.", StandardCharsets.UTF_8));
                        return null;
                    }

                    // Traer solo las mesas en las que está inscripto y que la fecha sea mayor a hoy
                    String today = LocalDate.now().toString();
                    String sql = "SELECT fs.id as sheet_id, s.name as subject_name, fs.year as exam_date, fs.call " +
                                 "FROM final_grades fg " +
                                 "INNER JOIN final_sheets fs ON fg.final_sheet_id = fs.id " +
                                 "INNER JOIN subjects s ON fs.subject_id = s.id " +
                                 "WHERE fg.student_id = ? AND fs.year > ? " +
                                 "ORDER BY fs.year ASC";
                    
                    List<Map> enrolledFinals = Base.findAll(sql, student.getId(), today);

                    model.put("finals", enrolledFinals);
                    model.put("hasFinals", !enrolledFinals.isEmpty());
                    model.put("tituloPagina", "Mis Inscripciones a Finales");
                }

                String success = req.queryParams("success");
                if (success != null) model.put("successMessage", success);
                
                String error = req.queryParams("error");
                if (error != null) model.put("errorMessage", error);

                return new ModelAndView(model, "student_unenroll_final.mustache");

            } catch (Exception e) {
                e.printStackTrace();
                model.put("errorMessage", "Error al cargar tus inscripciones.");
                return new ModelAndView(model, "student_unenroll_final.mustache");
            }
        }, new MustacheTemplateEngine());



        post("/academic-performance", (req, res) -> { //REUBICAR
            Map<String, Object> model = new HashMap<>();
            String planId = req.queryParams("plan_id");
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));
            String subjectsQuery = "SELECT s.code, s.name, fs.year, fg.grade " + 
                                "FROM (SELECT * FROM enrolled_plan WHERE plan_id = ? AND student_id = ?) AS ep " + //Subconsulta para plan seleccionado
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + //Materias del plan
                                "INNER JOIN enrolled_subject es ON es.student_id = ep.student_id AND sbp.subject_id = es.subject_id " + //Materias del alumno y del plan
                                "INNER JOIN subjects s ON es.subject_id = s.id " + //Materias (para sacar los datos)
                                "INNER JOIN final_sheets fs ON s.id = fs.subject_id " +  
                                "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id " + 
                                "WHERE fg.student_id = ?";

            String mode = req.queryParams("mode");
            String gradeMode = "";
            boolean both = false;
            if(mode.equals("aprobadas")){
                gradeMode = " AND fg.grade >= 5";
            } else if (mode.equals("desaprobadas")){
                gradeMode = " AND fg.grade < 5";
            } else {
                both = true;
            }
            
            List<Map> subjects = Base.findAll(subjectsQuery + gradeMode, planId, student.getId(), student.getId());
            List<Map> allSubjects = Base.findAll(subjectsQuery, planId, student.getId(), student.getId());

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

            model.put("subjects", subjects);
            model.put("hasSubjects", !allSubjects.isEmpty());
            model.put("mode", mode);
            model.put("both", both);
            model.put("totalAverage", totalAverage);
            model.put("approvedAverage", approvedAverage);
            return new ModelAndView(model, "academic_performance.mustache");
        }, new MustacheTemplateEngine());
        
        // --- Rutas POST para manejar envíos de formularios y APIs ---



        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla de login o dashboard.
            model.put("tituloPagina","Iniciar Sesión");

            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            // Validaciones básicas: campos de usuario y contraseña no pueden ser nulos o vacíos.
            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400); // Bad Request.
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }

            // Busca la cuenta en la base de datos por el nombre de usuario.
            User ac = User.findFirst("name = ?", username);

            // Si no se encuentra ninguna cuenta con ese nombre de usuario.
            if (ac == null) {
                res.status(401); // Unauthorized.
                model.put("errorMessage", "Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }

            // Obtiene la contraseña hasheada almacenada en la base de datos.
            String storedHashedPassword = ac.getString("password");

            // Compara la contraseña en texto plano ingresada con la contraseña hasheada almacenada.
            // BCrypt.checkpw hashea la plainTextPassword con el salt de storedHashedPassword y compara.
            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                // Autenticación exitosa.
                res.status(200); // OK.

                Integer personId = ac.getInteger("person_id");
                Integer isAdminInt = ac.getInteger("is_admin");
                Boolean isAdmin = isAdminInt != null && isAdminInt == 1;

                // -- Detectar roles -- 
                Student studentModel = Student.findFirst("person_id = ?", personId);
                boolean isStudent = studentModel != null;
                boolean isTeacher = Teacher.findFirst("person_id = ?", personId) != null;
                boolean isRegularStudent = false;
                int roleCount = (isAdmin ? 1 : 0) + (isStudent ? 1 : 0) + (isTeacher ? 1:0);


                if(isStudent){
                    isRegularStudent = Enrolled_Plan.findFirst("student_id = ?",studentModel.getId()) != null;
                }

                // --- Gestión de Sesión ---
                req.session(true).attribute("currentUserUsername", username); // Guarda el nombre de usuario en la sesión.
                req.session().attribute("userId", ac.getId()); // Guarda el ID de la cuenta en la sesión (útil).
                req.session().attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está logueado.
                // Roles
                req.session().attribute("isAdmin", isAdmin); 
                req.session().attribute("isTeacher", isTeacher);
                req.session().attribute("isStudent", isStudent);
                req.session().attribute("isRegularStudent", isRegularStudent);
                // Asignar rol activo
                if(isAdmin) req.session().attribute("activeRole","ADMIN");
                else if(isTeacher) req.session().attribute("activeRole","TEACHER");
                else if(isStudent) req.session().attribute("activeRole","STUDENT");
                else req.session().attribute("activeRole","NONE");

                String activeRole = (String) req.session().attribute("activeRole");

                System.out.println("DEBUG Login exitoso para " + username + " (Admin: " + isAdmin + ")");
                
                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());
                System.out.println("DEBUG: Usuario con role: " + req.session().attribute("activeRole"));

                model.put("username", username); // Añade el nombre de usuario al modelo para el dashboard.
                model.put("isAdmin", isAdmin);
                model.put("isTeacher", isTeacher);
                model.put("isStudent", isStudent);
                model.put("hasMultipleRoles", roleCount > 1);
                model.put("activeRole",activeRole);            


                model.put("isActiveAdmin", "ADMIN".equals(activeRole));
                model.put("isActiveTeacher", "TEACHER".equals(activeRole));
                model.put("isActiveStudent", "STUDENT".equals(activeRole));

                model.put("tituloPagina", "Dashboard - Bienvenido");
                // Renderiza la plantilla del dashboard tras un login exitoso.
                return new ModelAndView(model, "dashboard.mustache");
            } else {
                // Contraseña incorrecta.
                res.status(401); // Unauthorized.
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
                return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
            }
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta POST.


        post("/subject/new", (req, res) -> {
            String id = req.queryParams("code"); 
            String name = req.queryParams("name");
            String respId = req.queryParams("responsible_id");
            String planId = req.queryParams("plan_id");

            if (id == null || name == null || respId == null || id.isEmpty() || name.isEmpty() || planId == null || planId.isEmpty()) {
                res.redirect("/subject/create?error=" + URLEncoder.encode("Faltan datos obligatorios", "UTF-8"));
                return "";
            }

            try {
                Base.openTransaction();
                Subject s = new Subject();
                s.set("code", Integer.parseInt(id));
                s.set("name", name);
                s.set("responsible_id", Integer.parseInt(respId));
                if (s.saveIt()) {
                    // si se guardó la materia, la asocio al plan
                    Plan p = Plan.findById(Integer.parseInt(planId));
                    if (p != null) {
                        s.add(p); // acá ActiveJDBC hace el insert a subject_belongs_plan, por el @Many2Many
                    }
                    Base.commitTransaction();
                    res.redirect("/subject/create?successMessage=" + URLEncoder.encode("Materia '" + name + "' creada y asignada con éxito :D", "UTF-8"));
                } else {
                    Base.rollbackTransaction();
                    res.redirect("/subject/create?errorMessage=" + URLEncoder.encode("Error de validación: " + s.errors(), "UTF-8"));
                }
            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/subject/create?errorMessage=" + URLEncoder.encode("Error: El código ya existe o es inválido", "UTF-8"));
            }
            return "";
        });

        post("/career/new", (req, res) -> {
           String name = req.queryParams("name").trim();
          
          
            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (name == null || name.isEmpty()){
               String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
               res.redirect("/career/create?error=" + errorMsg);
               return "";
            }

            //Validación de nombre
            String result = name.replaceAll("[^\\p{L}\\p{Nd}\\s]", ""); //Quita todos los caracteres especiales (que no son letras, números o espacios intermedios) del name
            if(result.length() != name.length()){ //Chequear si cambió la longitud
                String errorMsg = URLEncoder.encode("El nombre no puede contener caracteres especiales.", StandardCharsets.UTF_8);
                res.redirect("/career/create?error=" + errorMsg);
                return "";
            }

            //Principal
            try {
                // Intenta crear y guardar la nueva carrera en la base de datos.
                
                Base.openTransaction();  // Iniciamos la transaccion

                Career nc = new Career(); // Crea una nueva instancia del modelo Career.
                nc.set("name", name);
                nc.saveIt();

                Base.commitTransaction();               

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                String successMsg = URLEncoder.encode("Carrera "+name+" registrada correctamente.",StandardCharsets.UTF_8);
                res.redirect("/career/create?message= " + successMsg);
                return ""; // Retorna una cadena vacía.


           } catch (Exception e) {
               // Si ocurre cualquier error durante la operación de DB (ej. código de carrera duplicado),
               // se captura aquí y se redirige con un mensaje de error.
               Base.rollbackTransaction(); // Si falla algo deshace
               e.printStackTrace(); // Imprime el stack trace para depuración.
               res.status(500); // Código de estado HTTP 500 (Internal Server Error).
               String errorMsg = URLEncoder.encode("ERROR: id de carrera ya existente o error interno.", StandardCharsets.UTF_8);
               res.redirect("/career/create?error="+errorMsg);
               return ""; // Retorna una cadena vacía.
           }
        });


        post("/plan/new", (req, res) -> {
                     
            String careerId = req.queryParams("career_id"); // id de la carrera seleccionada
            String statePlan = req.queryParams("state");   //estado del plan
            String versionPlan = req.queryParams("version"); // version del plan
           
            // Validaciones básicas: campos no pueden ser nulos o vacíos.

             if (careerId == null || careerId.isEmpty()){
               String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
               res.redirect("/career/create?error=" + errorMsg);
               return "";
            }

             if (statePlan == null || statePlan.isEmpty()){
               String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
               res.redirect("/career/create?error=" + errorMsg);
               return "";

            }

            if (versionPlan == null || versionPlan.isEmpty()){
               String errorMsg = URLEncoder.encode("Todos los campos son requeridos.", StandardCharsets.UTF_8);
               res.redirect("/career/create?error=" + errorMsg);
               return "";  

            }

            //Principal
            try {
                // Intenta crear y guardar el nuevo plan de estudios  en la base de datos.
                
                Base.openTransaction();  // Iniciamos la transaccion

                Plan np = new Plan(); // Crea una nueva instancia del modelo PLan.
                
                np.set("career_id",careerId);
                np.set("state",statePlan);
                np.set("version",versionPlan);
                np.saveIt();

                Base.commitTransaction();               

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                String successMsg = URLEncoder.encode("Plan "+versionPlan+" registrado correctamente.",StandardCharsets.UTF_8);
                res.redirect("/plan/new?successMessage="+successMsg);
                return ""; // Retorna una cadena vacía.


           } catch (Exception e) {
               // Si ocurre cualquier error durante la operación de DB (ej. código de carrera duplicado),
               // se captura aquí y se redirige con un mensaje de error.
               Base.rollbackTransaction(); // Si falla algo deshace
               e.printStackTrace(); // Imprime el stack trace para depuración.
               res.status(500); // Código de estado HTTP 500 (Internal Server Error).
               String errorMsg = URLEncoder.encode("ERROR: id de plan de carrera ya existente o error interno.", StandardCharsets.UTF_8);
               res.redirect("/plan/new?errorMessage="+errorMsg);
               return ""; // Retorna una cadena vacía.
           }
        });


        post("/plan/update", (req, res) -> {
            String planId = req.queryParams("plan_id");
            String nuevoEstado = req.queryParams("state");

            System.out.println("DEBUG POST PLAN: planId=[" + planId + "] | estado=[" + nuevoEstado + "]");

            if (planId == null || planId.isEmpty() || nuevoEstado == null || nuevoEstado.isEmpty()) {
                res.redirect("/plan/update?errorMessage=" + URLEncoder.encode("Debes seleccionar un plan y un estado", "UTF-8"));
                return "";
            }

            try {
                Plan p = Plan.findById(Integer.parseInt(planId));
                if (p != null) {
                    p.set("state", nuevoEstado);
                    p.saveIt();
                    res.redirect("/plan/update?successMessage=" + URLEncoder.encode("El plan fue actualizado con éxito :D", "UTF-8"));
                } else {
                    res.redirect("/plan/update?errorMessage=" + URLEncoder.encode("El plan no existe", "UTF-8"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/plan/update?errorMessage=" + URLEncoder.encode("Error: ", "UTF-8"));
            }
            return "";
        });

        post("/student/remove", (req, res) -> {
            String id = req.queryParams("id");

            if(id == null || id.isEmpty()){
                res.redirect("/student/delete?error=" + URLEncoder.encode("Faltan datos obligatorios", "UTF-8"));
                return "";
            }

            try {
                Base.openTransaction();
                Student st = Student.findById(Integer.parseInt(id));
                if(st != null && st.delete()){
                    Base.commitTransaction();
                    String successMsg = URLEncoder.encode("Estudiante ["+id+"] eliminado correctamente.",StandardCharsets.UTF_8);
                    res.redirect("/student/delete?message= " + successMsg);
                    return "";

                } else {
                    Base.rollbackTransaction();
                    res.redirect("/student/delete?error=" + URLEncoder.encode("Error: Código inválido", "UTF-8"));
                    return "";
                }

            } catch(Exception e){
                Base.rollbackTransaction();
                e.printStackTrace();
                res.redirect("/student/delete?error=" + URLEncoder.encode("Error", "UTF-8"));
            }
            return "";
        });
        
        post("/student/new", (req, res) -> {
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

            //Principal
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
                    Student existingStudent = Student.findFirst("person_id = ?", p.getId());
                    if (existingStudent != null) {
                        Base.rollbackTransaction();
                        String errorMsg = URLEncoder.encode("Esta persona ya está registrada como estudiante.", StandardCharsets.UTF_8);
                        res.redirect("/student/create?error=" + errorMsg);
                        return "";
                    }
                }
                Student ac = new Student();
                ac.set("person_id", p.getId());
                ac.saveIt();
                User u = User.findFirst("name = ?", dniStr);
                String randomPassword = PasswordGenerator.generateSecurePassword(8);
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
                
                if (isNewUser) {
                    // El mail original con credenciales
                    try { EmailSender.sendGenericAccountCreationMail(email, dniStr, firstname, lastname, randomPassword); } 
                    catch (Exception e) { e.printStackTrace(); }
                } else {
                    // El usuario ya existía, le avisamos que le agregaron el perfil
                    try { EmailSender.sendStudentRoleAddedMail(email, firstname, lastname); } 
                    catch (Exception e) { e.printStackTrace(); }
                }

                res.status(201); 
                String successMsgText = "Estudiante " + firstname + " " + lastname + " registrado correctamente.";
                String successMsg = URLEncoder.encode(successMsgText, StandardCharsets.UTF_8);

                res.redirect("/student/create?message=" + successMsg);
                return "";

           } catch (Exception e) {
                Base.rollbackTransaction(); 
                e.printStackTrace(); 
                res.status(500); 
                String errorMsg = URLEncoder.encode("ERROR interno al procesar el registro.", StandardCharsets.UTF_8);
                res.redirect("/student/create?error=" + errorMsg);
                return ""; 
           }
        });

        post("/set-role", (req, res) -> {
            String selectedRole = req.queryParams("role");
            System.out.println(selectedRole);            
            if (selectedRole != null && !selectedRole.isEmpty()) {
                req.session().attribute("activeRole", selectedRole);
            }
            res.redirect("/dashboard");
            return "";
        });

        post("/recover-password", (req, res) -> {
            String email = req.queryParams("email");

            //Validación básica, el campo mail no puede ser nulo o vacío.
            if (email == null || email.isEmpty()) {
               String errorMsg = URLEncoder.encode("Por favor, ingrese su correo electrónico.", StandardCharsets.UTF_8);
               res.redirect("/recover-password?error=" + errorMsg);
               return "";
            }
            //Validación de mail
            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            if(!email.matches(emailRegex)) {
                String errorMsg = URLEncoder.encode("Ingrese un correo electrónico válido (ej: usuario@dominio.com).", StandardCharsets.UTF_8);
                res.redirect("/recover-password?error=" + errorMsg);
                return "";
            }

            System.out.println("Recuperando contraseña: " + email);            
            
            //Buscar a la persona por email en la tabla persons
            Person person = Person.findFirst("email = ?", email);
            //Si la persona no existe, lo ignoramos
            if (person != null) {
                //Buscar al usuario vinculado a esa persona
                User user = User.findFirst("person_id = ?", person.getId());
                if (user != null) {
                    //Generar código random
                    String rawCode = String.format("%06d", new java.util.Random().nextInt(999999));
                    //Guardar código en la DB
                    RecoverPasswordCode recovery = new RecoverPasswordCode();
                    recovery.set("user_id", user.getId());
                    recovery.set("code", rawCode);
                    recovery.saveIt();
                    //Enviar el mail de forma asíncrona
                    try {
                        EmailSender.sendRecoveryMail(email,rawCode);
                        System.out.println("Correo enviado a: " + email + " con código: " + rawCode);
                    } catch (Exception e) {
                        System.err.println("Error al enviar el correo:");
                        e.printStackTrace();
                    }
                }
            }
            //Se redirecciona siempre, incluso si no existía el mail, por cuestiones de seguridad
            res.redirect("/reset-password");
            return "";
        });

        post("/reset-password", (req, res) -> {
            String token = req.queryParams("token");
            String newPassword = req.queryParams("newPassword");
            String confirmPassword = req.queryParams("confirmPassword");

            //Validación de campos obligatorios
            if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
                String errorMsg = URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8);
                res.redirect("/reset-password?error=" + errorMsg);
                return "";
            }

            //Validación de coincidencia de contraseñas
            if (!newPassword.equals(confirmPassword)) {
                String errorMsg = URLEncoder.encode("Las contraseñas no coinciden.", StandardCharsets.UTF_8);
                res.redirect("/reset-password?error=" + errorMsg);
                return "";
            }

            //Buscar el código en la DB
            RecoverPasswordCode recoveryRecord = RecoverPasswordCode.findFirst("code = ?", token);

            if (recoveryRecord == null) {
                String errorMsg = URLEncoder.encode("El código de verificación ingresado es incorrecto.", StandardCharsets.UTF_8);
                res.redirect("/reset-password?error=" + errorMsg);
                return "";
            }

            //Verificación de expiración del código (el límite es de 15 minutos)
            java.sql.Timestamp creationTime = recoveryRecord.getTimestamp("creation_time");
            long diferenciaMilisegundos = System.currentTimeMillis() - creationTime.getTime();
            long quinceMinutosEnMilisegundos = 15 * 60 * 1000;

            if (diferenciaMilisegundos > quinceMinutosEnMilisegundos) {
                recoveryRecord.delete(); // Eliminar el código expirado
                String errorMsg = URLEncoder.encode("El código expiró. Por favor, solicitá uno nuevo.", StandardCharsets.UTF_8);
                res.redirect("/recover-password?error=" + errorMsg);
                return "";
            }

            //Buscar el usuario asociado al código de recuperación
            Object userId = recoveryRecord.get("user_id");
            User user = User.findById(userId);

            if (user == null) {
                String errorMsg = URLEncoder.encode("No se pudo encontrar el usuario asociado.", StandardCharsets.UTF_8);
                res.redirect("/reset-password?error=" + errorMsg);
                return "";
            }

            //Actualización de contraseña e invalidación del token (en una transacción)
            try {
                String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
                user.set("password", hashedPassword);
                user.saveIt();

                recoveryRecord.delete();
                
                //Enviar mail de advertencia de contraseña cambiada
                Object personId = user.get("person_id");
                Person person = Person.findById(personId);
                String email = person.getMail();
                if (email != null && !email.isEmpty()) {
                    EmailSender.sendPasswordChangedWarning(email);
                }

                System.out.println("Contraseña restablecida para el usuario ID: " + userId);
                
                String successMsg = URLEncoder.encode("Contraseña restablecida con éxito. Ya podés iniciar sesión.", StandardCharsets.UTF_8);
                res.redirect("/?message=" + successMsg);

                return "";

            } catch (Exception e) {
                System.err.println("Error al actualizar la contraseña en la base de datos:");
                e.printStackTrace();
                String errorMsg = URLEncoder.encode("Ocurrió un error interno al procesar el cambio.", StandardCharsets.UTF_8);
                res.redirect("/reset-password?error=" + errorMsg);
                return "";
            }
        });

        post("/change-password", (req, res) -> {
            String currentPassword = req.queryParams("currentPassword");
            String newPassword = req.queryParams("newPassword");
            String confirmPassword = req.queryParams("confirmPassword");

            //Validar que las contraseñas nuevas coincidan
            if (newPassword == null || !newPassword.equals(confirmPassword)) {
                String errorMsg = URLEncoder.encode("Las contraseñas nuevas no coinciden.", StandardCharsets.UTF_8);
                res.redirect("/change-password?error=" + errorMsg);
                return "";
            }

            try {
                //Obtener user de la sesión
                Integer userId = req.session().attribute("userId");
                if (userId == null) {
                    String errorMsg = URLEncoder.encode("La sesión expiró. Volvé a logearte.", StandardCharsets.UTF_8);
                    res.redirect("/?error=" + errorMsg);
                    return "";
                }

                User user = User.findById(userId);
                if (user == null) {
                    res.redirect("/");
                    return "";
                }

                //Comparar el hash de la contraseña que puso el usuario con la que está en la DB
                String currentHash = user.getString("password");
                if (!org.mindrot.jbcrypt.BCrypt.checkpw(currentPassword, currentHash)) {
                    String errorMsg = URLEncoder.encode("La contraseña actual es incorrecta.", StandardCharsets.UTF_8);
                    res.redirect("/change-password?error=" + errorMsg);
                    return "";
                }

                //Hashear la nueva contraseña y guardarla
                String newHash = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
                user.set("password", newHash);
                user.saveIt();

                //Enviar el mail de advertencia de cambio de contraseña
                Object personId = user.get("person_id");
                Person person = Person.findById(personId);
                
                if (person != null) {
                    String email = person.getString("email");
                    if (email != null && !email.isEmpty()) {
                        EmailSender.sendPasswordChangedWarning(email);
                    }
                }

                String successMsg = URLEncoder.encode("Contraseña actualizada con éxito.", StandardCharsets.UTF_8);
                res.redirect("/change-password?success=" + successMsg);
                return "";

            } catch (Exception e) {
                System.err.println("Error al cambiar contraseña:");
                e.printStackTrace();
                
                String errorMsg = URLEncoder.encode("Ocurrió un error interno al procesar el cambio.", StandardCharsets.UTF_8);
                res.redirect("/change-password?error=" + errorMsg);
                return "";
            }
        });

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
                req.session().attribute("error", "Error: Alumno o materia no encontrados.");
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

                //Obtener el verdadero año actual
                int currentYear = Year.now().getValue();

                //Buscar el acta de cursado de la materia para el ciclo lectivo actual
                String gsSql = "SELECT id FROM grade_sheets WHERE subject_id = ? AND year = ? LIMIT 1";
                Object gradeSheetId = Base.firstCell(gsSql, subject.getId(), currentYear);

                //Si la gradesheet no existe, se crea en el momento 
                if (gradeSheetId == null) {
                    Base.exec("INSERT INTO grade_sheets (subject_id, year) VALUES (?, ?)", subject.getId(), currentYear);
                    //Recuperamos el ID que se le acaba de asignar
                    gradeSheetId = Base.firstCell(gsSql, subject.getId(), currentYear);
                }

                //Insertar el estado del alumno a la planilla
                Base.exec("INSERT INTO statuses (grade_sheet_id, student_id, initial_condition, final_condition) VALUES (?, ?, ?, ?)", 
                          gradeSheetId, student.getId(), "INSCRIPTO", "INSCRIPTO");

                Base.commitTransaction();
                req.session().attribute("success", "¡Te inscribiste con éxito a " + subject.getString("name") + "!");
                
            } catch (Exception e) {
                Base.rollbackTransaction();
                req.session().attribute("error", "Ocurrió un error en el servidor al procesar la solicitud.");
                e.printStackTrace();
            }

            res.redirect("/enrollment");
            return null;
        });

        post("/plans", (req, res) -> {
            String planId = req.queryParams("plan_id");
            Map<String, Object> model = new HashMap<>();
            Plan plan = Plan.findById(planId);

            model.put("plan", plan);
            
            //Materias pertenecientes al plan, sus correlativas y condiciones para cursar y rendir
            String subjectsQuery = 
                "SELECT s.id AS subj_id, s.code, s.name, corr.code AS correlative_code, " +
                    "c.course_condition, c.exam_condition " +
                "FROM subject_belongs_plan sb " +
                "INNER JOIN subjects s ON s.id = sb.subject_id " +
                "LEFT JOIN conditions c ON c.subject_id = sb.subject_id " +
                "LEFT JOIN subjects corr ON c.correlative_id = corr.id " +
                "WHERE sb.plan_id = ? " +
                "ORDER BY s.code, corr.code"; 

            List<Map> rawSubjects = Base.findAll(subjectsQuery, planId);

            List<Map<String, Object>> processedSubjects = new ArrayList<>();
            Object lastSubjectId = null;
            Map<String, Object> previousRow = null; // Nueva variable para rastrear la fila de arriba

            for (Map row : rawSubjects) {
                Map<String, Object> newRow = new HashMap<>(row);
                Object currentId = row.get("subj_id");
                // Por defecto, asumo que esta fila cierra el grupo y lleva borde
                newRow.put("has_border", true);
                if (currentId != null && currentId.equals(lastSubjectId)) {
                    newRow.put("code", "");
                    newRow.put("name", "");
                    // la fila de arriba no va a dibujar la línea inferior
                    if (previousRow != null) {
                        previousRow.put("has_border", false);
                    }
                } else {
                    lastSubjectId = currentId;
                }
                processedSubjects.add(newRow);
                previousRow = newRow; // Guarda la fila actual para la próxima vuelta
            }
            model.put("subjects", processedSubjects);

            return new ModelAndView(model, "plan_details.mustache");
        }, new MustacheTemplateEngine());

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


        post("/profile/verify-email", (req, res) -> {
            String inputCode = req.queryParams("code");
            String realCode = req.session().attribute("emailVerificationCode");
            String pendingEmail = req.session().attribute("pendingEmail");
            Object userIdAttr = req.session().attribute("userId");

            // Verificaciones
            if (userIdAttr == null) {
                res.redirect("/");
                return "";
            }

            if (inputCode == null || realCode == null || pendingEmail == null) {
                res.redirect("/profile?error=" + URLEncoder.encode("La sesión de verificación expiró o es inválida.", StandardCharsets.UTF_8));
                return "";
            }

            if (!inputCode.trim().equals(realCode)) {
                res.redirect("/profile/verify-email?error=" + URLEncoder.encode("El código es incorrecto. Intentá nuevamente.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                User user = User.findById(userIdAttr);
                if (user != null) {
                    Person person = Person.findById(user.get("person_id"));
                    if (person != null) {
                        person.set("email", pendingEmail);
                        person.saveIt();
                        
                        // Limpiamos la sesión para que no pueda reutilizar el código
                        req.session().removeAttribute("pendingEmail");
                        req.session().removeAttribute("emailVerificationCode");

                        res.redirect("/profile?success=" + URLEncoder.encode("Correo actualizado exitosamente.", StandardCharsets.UTF_8));
                        return "";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/profile?error=" + URLEncoder.encode("Ocurrió un error al intentar actualizar el correo en la base de datos.", StandardCharsets.UTF_8));
                return "";
            }
            return "";
        });

        post("/profile/update-email", (req, res) -> {
            String newEmail = req.queryParams("newEmail");
            Object userIdAttr = req.session().attribute("userId");

            if (userIdAttr == null) {
                res.redirect("/");
                return "";
            }

            if (newEmail == null || newEmail.trim().isEmpty()) {
                res.redirect("/profile?error=" + URLEncoder.encode("El correo no puede estar vacío.", StandardCharsets.UTF_8));
                return "";
            }

            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            if(!newEmail.matches(emailRegex)) {
                res.redirect("/profile?error=" + URLEncoder.encode("Formato de correo inválido.", StandardCharsets.UTF_8));
                return "";
            }

            String verificationCode = String.format("%06d", new java.util.Random().nextInt(999999));
            
            req.session().attribute("pendingEmail", newEmail.trim());
            req.session().attribute("emailVerificationCode", verificationCode);

            EmailSender.sendEmailChangeVerificationMail(newEmail.trim(), verificationCode);

            res.redirect("/profile/verify-email");
            return "";
        });

        // POST: Inscripción a plan de estudio
        post("/student/enroll-plan", (req, res) -> {
            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return "";
            }

            String planIdStr = req.queryParams("plan_id");
            if (planIdStr == null || planIdStr.trim().isEmpty()) {
                res.redirect("/student/enroll-plan?error=" + URLEncoder.encode("Debe seleccionar un plan de estudios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                Integer planId = Integer.parseInt(planIdStr);

                //Ver si ya está inscripto en ese plan
                Long count = Base.count("enrolled_plan", "student_id = ? AND plan_id = ?", student.getId(), planId);
                
                if (count > 0) {
                    res.redirect("/student/enroll-plan?error=" + URLEncoder.encode("Ya estás inscripto en esta carrera y plan.", StandardCharsets.UTF_8));
                    return "";
                }

                //Realizar la inscripción
                //Se usa Base.exec() porque enrolled_plan usa una PK compuesta y no tiene columna 'id'
                Base.exec("INSERT INTO enrolled_plan (student_id, plan_id) VALUES (?, ?)", student.getId(), planId);

                String successMsg = URLEncoder.encode("¡Inscripción exitosa a la carrera!", StandardCharsets.UTF_8);
                res.redirect("/student/enroll-plan?success=" + successMsg);

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/student/enroll-plan?error=" + URLEncoder.encode("Ocurrió un error al procesar tu inscripción.", StandardCharsets.UTF_8));
            }
            return "";
        });

        post("/student/subjects/grade", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String planId = req.queryParams("plan_id");
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));

            String sharedHead = "SELECT s.code, s.name, st.initial_condition";

            String sharedBody = " FROM (SELECT * FROM enrolled_plan WHERE plan_id = ? AND student_id = ?) AS ep " +
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + 
                                "INNER JOIN enrolled_subject es ON es.student_id = ep.student_id AND sbp.subject_id = es.subject_id " + 
                                "INNER JOIN subjects s ON es.subject_id = s.id " +
                                "INNER JOIN grade_sheets g ON s.id = g.subject_id " +
                                "INNER JOIN statuses st ON g.id = st.grade_sheet_id " +
                                "WHERE st.student_id = ? AND st.final_condition ";

            String currSubjectsQuery = sharedHead + sharedBody + "= 'INSCRIPTO'";

            String gradeSubjectsQuery = sharedHead + ", g.year, st.final_condition" + sharedBody + "<> 'INSCRIPTO'";

            List<Map> currSubjects = Base.findAll(currSubjectsQuery, planId, student.getId(), student.getId());
            List<Map> gradeSubjects = Base.findAll(gradeSubjectsQuery, planId, student.getId(), student.getId());

            model.put("currSubjects", currSubjects);
            model.put("gradeSubjects", gradeSubjects);
            model.put("currentYear", Year.now().getValue());
            return new ModelAndView(model, "grade_enrollments.mustache");
        }, new MustacheTemplateEngine());

        post("/student/subjects/final", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String planId = req.queryParams("plan_id");
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));

            String finalSubjectsQuery = "SELECT s.code, s.name, fs.call, fs.year, fg.grade " + 
                                "FROM (SELECT * FROM enrolled_plan WHERE plan_id = ? AND student_id = ?) AS ep " +
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + 
                                "INNER JOIN enrolled_subject es ON es.student_id = ep.student_id AND sbp.subject_id = es.subject_id " + 
                                "INNER JOIN subjects s ON es.subject_id = s.id " +
                                "INNER JOIN final_sheets fs ON s.id = fs.subject_id " + 
                                "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id " + 
                                "WHERE fg.student_id = ?";
            
            List<Map> finalSubjects = Base.findAll(finalSubjectsQuery, planId, student.getId(), student.getId());
            model.put("finalSubjects", finalSubjects);
            return new ModelAndView(model, "final_enrollments.mustache");
        }, new MustacheTemplateEngine());

        post("/academic-performance", (req, res) -> { 
            Map<String, Object> model = new HashMap<>();
            String planId = req.queryParams("plan_id");
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));
            String subjectsQuery = "SELECT s.code, s.name, fs.year, fg.grade " + 
                                "FROM (SELECT * FROM enrolled_plan WHERE plan_id = ? AND student_id = ?) AS ep " + //Subconsulta para plan seleccionado
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + //Materias del plan
                                "INNER JOIN enrolled_subject es ON es.student_id = ep.student_id AND sbp.subject_id = es.subject_id " + //Materias del alumno y del plan
                                "INNER JOIN subjects s ON es.subject_id = s.id " + //Materias (para sacar los datos)
                                "INNER JOIN final_sheets fs ON s.id = fs.subject_id " +  
                                "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id " + 
                                "WHERE fg.student_id = ?";

            String mode = req.queryParams("mode");
            String gradeMode = "";
            boolean both = false;
            if(mode.equals("aprobadas")){
                gradeMode = " AND fg.grade >= 5";
            } else if (mode.equals("desaprobadas")){
                gradeMode = " AND fg.grade < 5";
            } else {
                both = true;
            }
            
            List<Map> subjects = Base.findAll(subjectsQuery + gradeMode, planId, student.getId(), student.getId());
            List<Map> allSubjects = Base.findAll(subjectsQuery, planId, student.getId(), student.getId());

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

            model.put("subjects", subjects);
            model.put("hasSubjects", !allSubjects.isEmpty());
            model.put("mode", mode);
            model.put("both", both);
            model.put("totalAverage", totalAverage);
            model.put("approvedAverage", approvedAverage);
            return new ModelAndView(model, "academic_performance.mustache");
        }, new MustacheTemplateEngine());
        post("/final/new", (req, res) -> {
            String subjectIdStr = req.queryParams("subject_id");
            String dateStr = req.queryParams("date"); //Con formato YYYY-MM-DD
            String call = req.queryParams("call"); //"Primero", "Segundo", "Tercero"

            if (subjectIdStr == null || dateStr == null || call == null || dateStr.isEmpty()) {
                res.redirect("/final/new?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                LocalDate newExamDate = LocalDate.parse(dateStr);
                LocalDate hoy = LocalDate.now();

                //No se pueden crear mesas en el pasado
                if (newExamDate.isBefore(hoy)) {
                    res.redirect("/final/new?error=" + URLEncoder.encode("La fecha del examen no puede ser en el pasado.", StandardCharsets.UTF_8));
                    return "";
                }

                int examYear = newExamDate.getYear();

                //Traer las mesas de la materia que ya estén programadas para el mismo año corriente
                String sql = "SELECT * FROM final_sheets WHERE subject_id = ? AND year LIKE ?";
                List<FinalSheet> existingSheets = FinalSheet.findBySQL(sql, Integer.parseInt(subjectIdStr), examYear + "-%");

                //Validaciones de tiempo
                for (FinalSheet fs : existingSheets) {
                    String existingCall = fs.getString("call");
                    LocalDate existingDate = LocalDate.parse(fs.getString("year"));

                    //Evitar duplicados del mismo llamado en el mismo año
                    if (existingCall.equals(call)) {
                        res.redirect("/final/new?error=" + URLEncoder.encode("Ya existe un '" + call + " Llamado' para esta materia en el año " + examYear + ".", StandardCharsets.UTF_8));
                        return "";
                    }

                    //Orden de llamados (Primero < Segundo < Tercero)
                    if (call.equals("Primero")) {
                        if ((existingCall.equals("Segundo") || existingCall.equals("Tercero")) && newExamDate.isAfter(existingDate)) {
                            res.redirect("/final/new?error=" + URLEncoder.encode("El Primer Llamado debe ser ANTERIOR al Segundo/Tercer llamado.", StandardCharsets.UTF_8));
                            return "";
                        }
                    } else if (call.equals("Segundo")) {
                        if (existingCall.equals("Primero") && newExamDate.isBefore(existingDate)) {
                            res.redirect("/final/new?error=" + URLEncoder.encode("El Segundo Llamado debe ser POSTERIOR al Primer llamado.", StandardCharsets.UTF_8));
                            return "";
                        }
                        if (existingCall.equals("Tercero") && newExamDate.isAfter(existingDate)) {
                            res.redirect("/final/new?error=" + URLEncoder.encode("El Segundo Llamado debe ser ANTERIOR al Tercer llamado.", StandardCharsets.UTF_8));
                            return "";
                        }
                    } else if (call.equals("Tercero")) {
                        if ((existingCall.equals("Primero") || existingCall.equals("Segundo")) && newExamDate.isBefore(existingDate)) {
                            res.redirect("/final/new?error=" + URLEncoder.encode("El Tercer Llamado debe ser POSTERIOR a los llamados anteriores.", StandardCharsets.UTF_8));
                            return "";
                        }
                    }
                }

                Base.exec("INSERT INTO final_sheets (subject_id, year, call) VALUES (?, ?, ?)", 
                        Integer.parseInt(subjectIdStr), dateStr, call);
                
                res.redirect("/final/new?success=" + URLEncoder.encode("¡Mesa de examen creada exitosamente!", StandardCharsets.UTF_8));

            } catch (DateTimeParseException e) {
                res.redirect("/final/new?error=" + URLEncoder.encode("Formato de fecha inválido.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/final/new?error=" + URLEncoder.encode("Error interno al crear la mesa de examen.", StandardCharsets.UTF_8));
            }
            
            return "";
        });


        post("/student/enroll-final", (req, res) -> {
            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return "";
            }

            String finalSheetIdStr = req.queryParams("final_sheet_id");
            if (finalSheetIdStr == null || finalSheetIdStr.trim().isEmpty()) {
                res.redirect("/student/enroll-final?error=" + URLEncoder.encode("Debe seleccionar una mesa de examen.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                Integer finalSheetId = Integer.parseInt(finalSheetIdStr);

                Long count = Base.count("final_grades", "student_id = ? AND final_sheet_id = ?", student.getId(), finalSheetId);
                if (count > 0) {
                    res.redirect("/student/enroll-final?error=" + URLEncoder.encode("Ya estás inscripto en esta mesa de examen.", StandardCharsets.UTF_8));
                    return "";
                }

                Base.exec("INSERT INTO final_grades (final_sheet_id, student_id, grade) VALUES (?, ?, NULL)", finalSheetId, student.getId());

                String successMsg = URLEncoder.encode("Inscripcion exitosa", StandardCharsets.UTF_8);
                res.redirect("/student/enroll-final?success=" + successMsg);

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/student/enroll-final?error=" + URLEncoder.encode("Ocurrió un error al procesar tu inscripción.", StandardCharsets.UTF_8));
            }
            return "";
        });

        post("/student/unenroll-final", (req, res) -> {
            String currentUsername = req.session().attribute("currentUserUsername");
            if (currentUsername == null) {
                res.redirect("/login");
                return "";
            }

            String finalSheetIdStr = req.queryParams("final_sheet_id");
            if (finalSheetIdStr == null || finalSheetIdStr.trim().isEmpty()) {
                res.redirect("/student/unenroll-final?error=" + URLEncoder.encode("Debe seleccionar una mesa de examen válida.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                User user = User.findFirst("name = ?", currentUsername);
                Student student = Student.findFirst("person_id = ?", user.get("person_id"));
                Integer finalSheetId = Integer.parseInt(finalSheetIdStr);
                String today = LocalDate.now().toString();

                Long count = Base.count("final_sheets", "id = ? AND year > ?", finalSheetId, today);
                if (count == 0) {
                    res.redirect("/student/unenroll-final?error=" + URLEncoder.encode("No podés darte de baja. El plazo venció o la mesa no existe.", StandardCharsets.UTF_8));
                    return "";
                }

                int deleted = Base.exec("DELETE FROM final_grades WHERE student_id = ? AND final_sheet_id = ?", student.getId(), finalSheetId);
                
                if (deleted == 0)
                    res.redirect("/student/unenroll-final?error=" + URLEncoder.encode("No estabas inscripto en esta mesa.", StandardCharsets.UTF_8));
                else
                    res.redirect("/student/unenroll-final?success=" + URLEncoder.encode("Te diste de baja de la mesa de examen correctamente.", StandardCharsets.UTF_8));
                

            } catch (NumberFormatException e) {
                res.redirect("/student/unenroll-final?error=" + URLEncoder.encode("El ID de la mesa es inválido.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/student/unenroll-final?error=" + URLEncoder.encode("Ocurrió un error al procesar la baja de la mesa.", StandardCharsets.UTF_8));
            }
            return "";
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

    /**
     * Filtro de verificación de acceso a estudiantes.
     * Solo permite el acceso si el usuario tiene el flag 'isStudent' en true en la sesión.
     */
    private static void checkStudentAccess(spark.Request req, spark.Response res) {
        Boolean isStudent = (Boolean) req.session().attribute("isStudent");
        Boolean loggedIn = (Boolean) req.session().attribute("loggedIn");
        String currentUsername = req.session().attribute("currentUserUsername");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=" + URLEncoder.encode("Acceso restringido. Debes iniciar sesión.", StandardCharsets.UTF_8));
            halt(); 
            return;
        }

        if (isStudent == null || !isStudent) {
            System.out.println("DEBUG: Acceso a ruta de administrador denegado al usuario: " + currentUsername);
            res.redirect("/dashboard?error=" + URLEncoder.encode("Acceso denegado. Solo los estudiantes pueden acceder.", StandardCharsets.UTF_8));
            halt(); 
        }
    }

} // Fin de la clase App
