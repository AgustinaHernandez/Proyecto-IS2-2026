package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.

import spark.ModelAndView;
// Importaciones de Spark para renderizado de plantillas
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

import com.fasterxml.jackson.databind.ObjectMapper;
// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.controllers.*;
import com.is1.proyecto.models.*;
import com.is1.proyecto.models.controllers.TeacherControllerOld;
import com.is1.proyecto.utils.AccessControl;


/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 4567).
            
        // Obtener la instancia única del singleton de configuración de la base de datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        MustacheTemplateEngine engine = new MustacheTemplateEngine();

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
        before("/teacher/create", AccessControl::checkAdminAccess);
        before("/teacher/new", AccessControl::checkAdminAccess);
        before("/teacher/assign", AccessControl::checkAdminAccess);
        before("/subject/create", AccessControl::checkAdminAccess);
        before("/career/create", AccessControl::checkAdminAccess);
        before("/career/new", AccessControl::checkAdminAccess);
        before("/teacher/unassign", AccessControl::checkAdminAccess);
        before("/student/create", AccessControl::checkAdminAccess);


        // Filtro de seguridad que restringe el acceso solo a los estud
        before("/student/subject-enroll", AccessControl::checkStudentAccess);



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

        // Registrar rutas viejas de TeacherControllerOld. Trabajo de refactorizar pendiente.
        TeacherControllerOld.registrarRutas(engine, objectMapper);
        
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

        //GET: Consulta de rendimiento académico
        get("/student/academic-performance", StudentController::renderPerformanceForm, new MustacheTemplateEngine());
        post("/student/academic-performance", StudentController::handlePerformanceQuery, new MustacheTemplateEngine());
        
        //GET: Baja de Profesores
        get("/teacher/delete", TeacherController::renderDeleteForm, new MustacheTemplateEngine());
        //POST: Baja de Profesores
        post("/teacher/delete", TeacherController::handleDeleteTeacher);

        //GET: Modificación de profesores
        get("/teacher/modify", TeacherController::renderTeacherModify, new MustacheTemplateEngine());
        //POST: Modificación de profesores

        //POST: Guardado de datos modificados de profesores

        //GET: Alta de materia
        get("/subject/create", SubjectController::renderSubjectCreation, new MustacheTemplateEngine());
        //POST: Alta de materia
        post("/subject/create", SubjectController::handleSubjectCreation);     


        // GET: Muestra el formulario de inicio de sesión (login).
        get("/", AuthController::renderLoginForm, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.



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


        get("/student/subjects", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Object userId = req.session().attribute("userId");
            User user = User.findById(userId);
            Student student = Student.findFirst("person_id = ?", user.get("person_id"));

            List<Enrolled_Plan> enrolled = Enrolled_Plan.where("student_id = ?", student.getId()).include(Plan.class);

            model.put("enrolled", enrolled);
            return new ModelAndView(model, "choose_subjects.mustache");
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


        // --- Rutas POST para manejar envíos de formularios y APIs ---

        //GET: /logout
        get("/logout", AuthController::handleLogout);

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/", AuthController::handleLogin, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta POST

        post("/set-role", ProfileController::handleSetRole);


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


        
        // Inscripcion a materias
        get("/student/subject-enroll", EnrollmentController::renderEnrollmentForm, new MustacheTemplateEngine());
        post("/student/subject-enroll", EnrollmentController::handleEnrollment);

        
        get("/teacher/unassign", TeacherController::renderTeacherUnassign, new MustacheTemplateEngine());
        post("/teacher/unassign", TeacherController::handleTeacherUnassignation);


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

            String sharedHead = "SELECT s.code, s.name, st.initial_condition ";

            String sharedBody = " FROM enrolled_plan ep " +
                                "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " +
                                "INNER JOIN subjects s ON sbp.subject_id = s.id " +
                                "INNER JOIN grade_sheets g ON s.id = g.subject_id " +
                                "INNER JOIN statuses st ON g.id = st.grade_sheet_id AND st.student_id = ep.student_id " +
                                "WHERE ep.plan_id = ? AND ep.student_id = ? AND st.final_condition ";

            String currSubjectsQuery = sharedHead + sharedBody + "= 'INSCRIPTO'";

            String gradeSubjectsQuery = sharedHead + ", g.year, st.final_condition" + sharedBody + "<> 'INSCRIPTO'";

            List<Map> currSubjects = Base.findAll(currSubjectsQuery, planId, student.getId());
            List<Map> gradeSubjects = Base.findAll(gradeSubjectsQuery, planId, student.getId());

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

            String finalSubjectsQuery = 
                    "SELECT s.code, s.name, fs.call, fs.year, fg.grade " + 
                    "FROM enrolled_plan ep " +
                    "INNER JOIN subject_belongs_plan sbp ON ep.plan_id = sbp.plan_id " + 
                    "INNER JOIN subjects s ON sbp.subject_id = s.id " +
                    "INNER JOIN final_sheets fs ON s.id = fs.subject_id " + 
                    "INNER JOIN final_grades fg ON fs.id = fg.final_sheet_id AND fg.student_id = ep.student_id " + 
                    "WHERE ep.plan_id = ? AND ep.student_id = ?";
            
            List<Map> finalSubjects = Base.findAll(finalSubjectsQuery, planId, student.getId());
            model.put("finalSubjects", finalSubjects);
            return new ModelAndView(model, "final_enrollments.mustache");
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


} // Fin de la clase App
