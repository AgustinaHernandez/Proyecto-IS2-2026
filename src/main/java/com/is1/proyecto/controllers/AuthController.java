package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import com.is1.proyecto.services.AuthService;

/** Rutas --------------------------
 *  /login (GET/POST), 
 *  /logout (GET), 
 *  /recover-passwor  (GET/POST), 
 *  /reset-passwor  (GET/POST), 
 *  /change-passwor  (GET/POST).
 */

public class AuthController {
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
