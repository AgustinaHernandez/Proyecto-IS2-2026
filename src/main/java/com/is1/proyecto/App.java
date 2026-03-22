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
import com.is1.proyecto.models.*;


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
        // Las rutas protegidas revisan el atributo is_admin de la sesion
        before("/teacher/create", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/new", (req, res) -> checkAdminAccess(req, res));
        before("/teacher/assign", (req, res) -> checkAdminAccess(req, res));
        before("/subject/new", (req, res) -> checkAdminAccess(req, res));
        before("/career/create", (req, res) -> checkAdminAccess(req, res));
        before("/career/new", (req, res) -> checkAdminAccess(req, res));

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

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query parameters.
        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        get("/teacher/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

            // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "teacher_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

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
                "teachers", teachers,
                "subjects", subjects, 
                "searchQuery", searchQuery != null ? searchQuery : "",
                "errorMessage", req.queryParamOrDefault("errorMessage", ""),
                "successMessage", req.queryParamOrDefault("successMessage", "")
            );
            
            return new ModelAndView(model, "teacher_assign_form.mustache");
        }, new MustacheTemplateEngine());

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
                "teachers", teachers,
                "query", (query != null)? query : "",
                "offset", offset,
                "successMessage", req.queryParamOrDefault("message", ""),
                "errorMessage", req.queryParamOrDefault("error", "")
            );

            return new ModelAndView(model, "teacher_delete.mustache");
        }, new MustacheTemplateEngine());

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

            // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            Boolean is_admin = (Boolean) req.session().attribute("is_admin"); 
            
            // 1. Verificar si el usuario ha iniciado sesión.
            // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
            // significa que el usuario no está logueado o su sesión expiró.
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
                // Redirige al login con un mensaje de error.
                res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
                return null; // Importante retornar null después de una redirección.
            }

            // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
            model.put("username", currentUsername);
            model.put("is_admin", is_admin != null && is_admin);
            
            // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.
        // GET: Página de configuración
        get("/settings", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            // Verificar sesión (opcional, pero recomendado)
            if (req.session().attribute("loggedIn") == null) {
                res.redirect("/login");
                return null;
            }

            return new ModelAndView(model, "settings.mustache");
        }, new MustacheTemplateEngine());
        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo hace, aplicar similar).
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

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create' para evitar duplicidad.
        get("/user/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "user_form.mustache"); // No pasa un modelo específico, solo el formulario.
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


        get("/profile", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Object rawUserId = req.session().attribute("userId");
            String currentUsername = req.session().attribute("currentUserUsername");
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
                res.redirect("/?error=Debes iniciar sesión para acceder a tu perfil.");
                return null;
            }

            User currentUser = User.findById(userId);

            if (currentUser == null) {
                res.redirect("/logout");
                return null;
            }

            model.put("userId", currentUser.getId());
            model.put("username", currentUsername); 
            
            return new ModelAndView(model, "perfil_usuario.mustache");
        }, new MustacheTemplateEngine());


        get("/subject/create", (req, res) -> {
            //checkAdminAccess(req, res);
            // select de todos los profesores con sus datos de la tabla persona
            List<Teacher> teachers = Teacher.findAll().include(Person.class);
            // buscamos los planes
            List<Plan> plans = Plan.findAll().include(Career.class); 
            // mapeo para pasarle al mustache luego
            Map<String, Object> model = Map.of(
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

            // Renderiza la plantilla 'career_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.


        // --- Rutas POST para manejar envíos de formularios y APIs ---

        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", (req, res) -> {
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // Validaciones básicas: campos no pueden ser nulos o vacíos.
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Código de estado HTTP 400 (Bad Request).
                // Redirige al formulario de creación con un mensaje de error.
                res.redirect("/user/create?error=Nombre y contraseña son requeridos.");
                return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
            }

            try {
                // Intenta crear y guardar la nueva cuenta en la base de datos.
                User ac = new User(); // Crea una nueva instancia del modelo User.
                // Hashea la contraseña de forma segura antes de guardarla.
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                ac.set("name", name); // Asigna el nombre de usuario.
                ac.set("password", hashedPassword); // Asigna la contraseña hasheada.
                ac.set("is_admin", 0);
                ac.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Código de estado HTTP 201 (Created) para una creación exitosa.
                // Redirige al formulario de creación con un mensaje de éxito.
                res.redirect("/user/create?message=Cuenta creada exitosamente para " + name + "!");
                return ""; // Retorna una cadena vacía.

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
                // se captura aquí y se redirige con un mensaje de error.
                System.err.println("Error al registrar la cuenta: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Código de estado HTTP 500 (Internal Server Error).
                res.redirect("/user/create?error=Error interno al crear la cuenta. Intente de nuevo.");
                return ""; // Retorna una cadena vacía.
            }
        });

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
            String result = firstname.replaceAll("\\d", ""); //Quitar todos los números del firstname
            if(result.length() != firstname.length()){ //Chequear si cambió la longitud
                String errorMsg = URLEncoder.encode("El nombre no puede contener números.", StandardCharsets.UTF_8);
                res.redirect("/teacher/create?error=" + errorMsg);
                return "";
            }
            //Validación de apellido
            result = lastname.replaceAll("\\d", ""); //Quitar todos los números del lastname
            if(result.length() != lastname.length()){ //Chequear si cambió la longitud
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
               //Chequear si existe una persona con el mismo DNI o gmail. Si no, crearla.
               //Chequear si esa persona ya está registrada como profesor.
               //Si es así, denegar la solicitud. Sino, registrarla como profesor.


               // Intenta crear y guardar la nueva cuenta en la base de datos.
            
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

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla de login o dashboard.

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

                Integer is_adminInt = ac.getInteger("is_admin");
                Boolean is_adminUser = is_adminInt != null && is_adminInt == 1; 

                // --- Gestión de Sesión ---
                req.session(true).attribute("currentUserUsername", username); // Guarda el nombre de usuario en la sesión.
                req.session().attribute("userId", ac.getId()); // Guarda el ID de la cuenta en la sesión (útil).
                req.session().attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está logueado.
                req.session().attribute("is_admin", is_adminUser); 

                System.out.println("DEBUG Login exitoso para " + username + " (Admin: " + is_adminUser + ")");
                
                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());


                model.put("username", username); // Añade el nombre de usuario al modelo para el dashboard.
                model.put("is_admin", is_adminUser);
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


        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // --- Validaciones básicas ---
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                User newUser = new User(); // Crea una nueva instancia de tu modelo User.
                // ¡ADVERTENCIA DE SEGURIDAD CRÍTICA!
                // En una aplicación real, las contraseñas DEBEN ser hasheadas (ej. con BCrypt)
                // ANTES de guardarse en la base de datos, NUNCA en texto plano.
                // (Nota: El código original tenía la contraseña en texto plano aquí.
                // Se recomienda usar `BCrypt.hashpw(password, BCrypt.gensalt())` como en la ruta '/user/new').
                newUser.set("name", name); // Asigna el nombre al campo 'name'.
                newUser.set("password", password); // Asigna la contraseña al campo 'password'.
                newUser.set("is_admin", 0);
                newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));

            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println("Error al registrar usuario: " + e.getMessage());
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper.writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
            }
        });

        post("/subject/new", (req, res) -> {
            String id = req.queryParams("code"); 
            String name = req.queryParams("name");
            String respId = req.queryParams("responsible_id");
            String planId = req.queryParams("plan_id");

            if (id == null || name == null || respId == null || id.isEmpty() || name.isEmpty() || planId == null) {
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

    } // Fin del método main

    /**
     * Filtro de verificación de acceso administrativo.
     * Solo permite el acceso si el usuario tiene el flag 'is_admin' en true en la sesión.
     */
    private static void checkAdminAccess(spark.Request req, spark.Response res) {
        Boolean is_admin = (Boolean) req.session().attribute("is_admin");
        Boolean loggedIn = (Boolean) req.session().attribute("loggedIn");
        String currentUsername = req.session().attribute("currentUserUsername");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=" + URLEncoder.encode("Acceso restringido. Debes iniciar sesión.", StandardCharsets.UTF_8));
            halt(); 
            return;
        }

        if (is_admin == null || !is_admin) {
            System.out.println("DEBUG: Acceso a ruta de administrador denegado al usuario: " + currentUsername);
            res.redirect("/dashboard?error=" + URLEncoder.encode("Acceso denegado. Solo el administrador puede registrar profesores.", StandardCharsets.UTF_8));
            halt(); 
        }
    }
} // Fin de la clase App
