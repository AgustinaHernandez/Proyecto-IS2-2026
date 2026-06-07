package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import com.is1.proyecto.services.AuthService;
import com.is1.proyecto.services.AuthService.LoginServiceResult;
import com.is1.proyecto.utils.InputValidator;

/** Rutas --------------------------
 *  /login (GET/POST), 
 *  /logout (GET), 
 *  /recover-passwor  (GET/POST), 
 *  /reset-passwor  (GET/POST), 
 *  /change-passwor  (GET/POST).
 */

public class AuthController {

    // GET: login (/)
    public static ModelAndView renderLoginForm(Request req, Response res) {
        Map<String, Object> model = Map.of(
            "errorMessage", req.queryParamOrDefault("error", ""),
            "successMessage", req.queryParamOrDefault("message", "")
        );
        return new ModelAndView(model, "login.mustache");
    }

    // POST: /login
    public static ModelAndView handleLogin(Request req, Response res) {
        String username = req.queryParams("username");
        String plainTextPassword = req.queryParams("password");

        LoginServiceResult serviceResult = AuthService.authenticate(username, plainTextPassword);

        if (!serviceResult.success) {
            res.status(serviceResult.status);
            if (serviceResult.status != 400 && serviceResult.ac != null) {
                // Contraseña incorrecta.
                System.out.println("DEBUG: Intento de login fallido para: " + username);
            }
            return new ModelAndView(Map.of(
                "tituloPagina", "Iniciar Sesión",
                "errorMessage", serviceResult.errorMessage
            ), "login.mustache"); // Renderiza la plantilla de login con error.
        }

        res.status(serviceResult.status);

        // --- Gestión de Sesión ---
        req.session(true).attribute("currentUserUsername", username); // Guarda el nombre de usuario en la sesión.
        req.session().attribute("userId", serviceResult.ac.getId()); // Guarda el ID de la cuenta en la sesión (útil).
        req.session().attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está logueado.
        // Roles
        req.session().attribute("isAdmin", serviceResult.isAdmin); 
        req.session().attribute("isTeacher", serviceResult.isTeacher);
        req.session().attribute("isStudent", serviceResult.isStudent);
        req.session().attribute("isRegularStudent", serviceResult.isRegularStudent);
        // Asignar rol activo
        if(serviceResult.isAdmin) req.session().attribute("activeRole","ADMIN");
        else if(serviceResult.isTeacher) req.session().attribute("activeRole","TEACHER");
        else if(serviceResult.isStudent) req.session().attribute("activeRole","STUDENT");
        else req.session().attribute("activeRole","NONE");

        String activeRole = (String) req.session().attribute("activeRole");

        System.out.println("DEBUG Login exitoso para " + username + " (Admin: " + serviceResult.isAdmin + ")");
        
        System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
        System.out.println("DEBUG: ID de Sesión: " + req.session().id());

        // Renderiza la plantilla del dashboard tras un login exitoso.
        ModelAndView model = new ModelAndView(Map.of(
            "username", username,
            "isAdmin", serviceResult.isAdmin,
            "isTeacher", serviceResult.isTeacher,
            "isStudent", serviceResult.isStudent,
            "hasMultipleRoles", serviceResult.roleCount > 1,
            "activeRole", activeRole,
            "isActiveAdmin", "ADMIN".equals(activeRole),
            "isActiveTeacher", "TEACHER".equals(activeRole),
            "isActiveStudent", "STUDENT".equals(activeRole),
            "tituloPagina", "Dashboard - Bienvenido"
        ), "dashboard.mustache");

        return model;
    }


    // GET: /logout
    // (mantengo los comentarios originales del App.java)
    public static Object handleLogout(Request req, Response res) {
        // Invalida completamente la sesión del usuario.
        // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
        // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
        req.session().invalidate();

        System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /.");

        // Redirige al usuario a la página de login con un mensaje de éxito.
        res.redirect("/");

        return null; // Importante retornar null después de una redirección.
    }


    // GET change-password
    public static ModelAndView renderChangePassword(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /change-password. Redirigiendo a /login.");
            res.redirect("/?error=Acceso+no+autorizado.");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("tituloPagina", "Cambiar contraseña");
        model.put("errorMessage", req.queryParamOrDefault("error", ""));
        model.put("successMessage", req.queryParamOrDefault("success", ""));

        return new ModelAndView(model, "change_password.mustache");
    }

    // POST change-password
    public static Object handleChangePassword(Request req, Response res) {
        Integer userId = req.session().attribute("userId");
        
        if (userId == null) {
            res.redirect("/?error=" + URLEncoder.encode("La sesión expiró. Volvé a logearte.", StandardCharsets.UTF_8));
            return "";
        }

        String currentPassword = req.queryParams("currentPassword");
        String newPassword = req.queryParams("newPassword");
        String confirmPassword = req.queryParams("confirmPassword");

        // Llamamos a la capa de servicio
        String errorMsg = AuthService.changePassword(userId, currentPassword, newPassword, confirmPassword);

        if (errorMsg != null) {
            res.redirect("/change-password?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        } else {
            res.redirect("/change-password?success=" + URLEncoder.encode("Contraseña actualizada con éxito.", StandardCharsets.UTF_8));
        }
        
        return "";
    }

    //GET recover-password
    public static ModelAndView renderRecoverPassword(Request req, Response res) {
        Map<String, Object> model = Map.of(
                "tituloPagina", "Recuperar contraseña",
                "errorMessage", req.queryParamOrDefault("error", ""),
                "successMessage", req.queryParamOrDefault("message", "")
        );

        return new ModelAndView(model, "recover_password.mustache");
    }

    //POST recover-password
    public static Object handleRecoverPasswordRequest(Request req, Response res) {
        String email = req.queryParams("email");
        
        String emailValidationError = InputValidator.validateEmail(email);
        if(emailValidationError != null){
            res.redirect("/recover-password?error=" + emailValidationError);
        }

        System.out.println("Recuperando contraseña: " + email);            
        
        AuthService.recoverPassword(email);

        //Se redirecciona siempre, incluso si no existía el mail, por cuestiones de seguridad
        res.redirect("/reset-password");
        return "";
    }

    //GET reset-password
    public static ModelAndView renderResetPassword(Request req, Response res) {
        Map<String, Object> model = Map.of(
            "tituloPagina", "Ingresar código",
            "errorMessage", req.queryParamOrDefault("error", ""),
            "successMessage", req.queryParamOrDefault("message", "")
        );

        return new ModelAndView(model, "reset_password.mustache");
    }

    //POST reset-password
    public static Object handleResetPasswordRequest(Request req, Response res) {
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

        String errorMsg = AuthService.resetPassword(token,newPassword);

        if (errorMsg != null) {
            res.redirect("/reset-password?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        } else {
            res.redirect("/?message=" + URLEncoder.encode("Contraseña restablecida con éxito. Ya podés iniciar sesión.", StandardCharsets.UTF_8));
        }
        
        return "";
    }
}
