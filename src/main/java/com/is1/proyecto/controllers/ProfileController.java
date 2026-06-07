package com.is1.proyecto.controllers;

import com.is1.proyecto.services.ProfileService;
import com.is1.proyecto.services.ProfileService.ProfileDataResult;
import com.is1.proyecto.utils.EmailSender;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Rutas --------------------------
 * /dashboard (GET), 
 * /set-role (POST), 
 * /profile (GET), 
 * /settings (GET), 
 * /profile/update-email (POST),     ---
 * /profile/verify-email (GET/POST). ---
 */


public class ProfileController {

    //POST: set-role
    public static Object handleSetRole(Request req, Response res) {
        String selectedRole = req.queryParams("role");
        System.out.println(selectedRole);            
        
        if (selectedRole != null && !selectedRole.isEmpty()) {
            req.session().attribute("activeRole", selectedRole);
        }
        
        res.redirect("/dashboard");
        return "";
    }

    //GET: /settings
    public static ModelAndView renderSettings(Request req, Response res) {
        // Verificar sesión
        if (req.session().attribute("loggedIn") == null) {
            res.redirect("/login");
            return null;
        }
        
        return new ModelAndView(Map.of(
            "tituloPagina", "Configuración"
        ), "settings.mustache");
    }

    // GET /profile
    public static ModelAndView renderProfile(Request req, Response res) {
        Object rawUserId = req.session().attribute("userId");
        String currentUsername = req.session().attribute("currentUserUsername");
        String role = req.session().attribute("activeRole");
        Long userId = null;

        // Validaciones de Sesión
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
            res.redirect("/?error=Acceso+no+autorizado.");
            return null;
        }

        // Delegamos a la capa de Servicio la extracción de datos de la DB
        ProfileDataResult data = ProfileService.getProfileData(userId, role);

        if (!data.success) {
            res.redirect("/logout");
            return null;
        }

        // Armamos el modelo usando Map.of pero en HashMap para poder agregar cosas nulas y params dinámicos
        Map<String, Object> model = new HashMap<>();
        model.put("userId", userId);
        model.put("username", currentUsername); 
        model.put("fullName", data.fullName);
        model.put("dni", data.dni);
        model.put("email", data.email);
        model.put("tituloPagina", "Perfil de Usuario");
        model.put("degree", data.degree); // Puede ser null, por eso usamos HashMap en lugar de Map.of

        String error = req.queryParams("error");
        if (error != null && !error.isEmpty()) {
            model.put("errorMessage", error);
        }
        String success = req.queryParams("success");
        if (success != null && !success.isEmpty()) {
            model.put("successMessage", success);
        }

        return new ModelAndView(model, "perfil_usuario.mustache");
    }


    // GET
    public static ModelAndView renderVerifyEmail(Request req, Response res) {
        if (req.session().attribute("userId") == null) {
            res.redirect("/login");
            return null;
        }

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
    }

    // POST
    public static Object handleUpdateEmailRequest(Request req, Response res) {
        Object userIdAttr = req.session().attribute("userId");
        if (userIdAttr == null) {
            res.redirect("/login");
            return "";
        }

        String newEmail = req.queryParams("newEmail");

        // 1. Usamos el Servicio para validar el texto puro
        String errorMsg = ProfileService.validateNewEmail(newEmail);
        if (errorMsg != null) {
            res.redirect("/profile?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
            return "";
        }

        String verificationCode = String.format("%06d", new java.util.Random().nextInt(999999));
        
        req.session().attribute("pendingEmail", newEmail.trim());
        req.session().attribute("emailVerificationCode", verificationCode);

        EmailSender.sendEmailChangeVerificationMail(newEmail.trim(), verificationCode);

        res.redirect("/profile/verify-email");
        return "";
    }

    // POST
    public static Object handleVerifyEmailCode(Request req, Response res) {
        Object userIdAttr = req.session().attribute("userId");
        if (userIdAttr == null) {
            res.redirect("/login");
            return "";
        }

        String inputCode = req.queryParams("code");
        String realCode = req.session().attribute("emailVerificationCode");
        String pendingEmail = req.session().attribute("pendingEmail");

        if (inputCode == null || realCode == null || pendingEmail == null) {
            res.redirect("/profile?error=" + URLEncoder.encode("La sesión de verificación expiró o es inválida.", StandardCharsets.UTF_8));
            return "";
        }

        if (!inputCode.trim().equals(realCode)) {
            res.redirect("/profile/verify-email?error=" + URLEncoder.encode("El código es incorrecto. Intentá nuevamente.", StandardCharsets.UTF_8));
            return "";
        }

        String errorMsg = ProfileService.updateEmailInDatabase(userIdAttr, pendingEmail);

        if (errorMsg != null) {
            res.redirect("/profile?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        } else {
            req.session().removeAttribute("pendingEmail");
            req.session().removeAttribute("emailVerificationCode");
            res.redirect("/profile?success=" + URLEncoder.encode("Correo actualizado exitosamente.", StandardCharsets.UTF_8));
        }
        
        return "";
    }
}