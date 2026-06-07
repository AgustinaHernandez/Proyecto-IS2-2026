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
// Importaciones estándar de Java
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.List;
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.

// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.controllers.*;
import com.is1.proyecto.models.*;
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
        before("/subject/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/new", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/unassign", (req, res) -> checkAdminAccess(req, res));

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

/*
        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });
 */
/*
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
 */


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
            if(role.equals("TEACHER")){ //Si es teacher, tiene título además de los otros datos
                List<Teacher> teacher = Teacher.find("person_id = ?", currentPerson.getID());
                degree = teacher.get(0).getDegree();         
            }
            model.put("degree", degree); //Si lo mapea como null, el formulario lo detecta y no lo muestra (ver perfil_usuario.mustache)
            return new ModelAndView(model, "perfil_usuario.mustache");
        }, new MustacheTemplateEngine());

/*
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
 */

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


        // --- Rutas POST para manejar envíos de formularios y APIs ---


/*
        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/", (req, res) -> {
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
*/
        
/*
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
 */

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
/*
        post("/set-role", (req, res) -> {
            String selectedRole = req.queryParams("role");
            System.out.println(selectedRole);            
            if (selectedRole != null && !selectedRole.isEmpty()) {
                req.session().attribute("activeRole", selectedRole);
            }
            res.redirect("/dashboard");
            return "";
        });
*/

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
