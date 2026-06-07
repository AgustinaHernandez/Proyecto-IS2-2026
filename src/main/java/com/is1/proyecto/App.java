package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.

// Importaciones de Spark para renderizado de plantillas
import spark.template.mustache.MustacheTemplateEngine; // Motor de plantillas Mustache para Spark.


// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.controllers.*;
import com.is1.proyecto.utils.AccessControl;


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

        
        // Inscripcion a materias
        get("/student/subject-enroll", EnrollmentController::renderEnrollmentForm, new MustacheTemplateEngine());
        post("/student/subject-enroll", EnrollmentController::handleEnrollment);

        
        get("/teacher/unassign", TeacherController::renderTeacherUnassign, new MustacheTemplateEngine());
        post("/teacher/unassign", TeacherController::handleTeacherUnassignation);




    } // Fin del método main

} // Fin de la clase App
