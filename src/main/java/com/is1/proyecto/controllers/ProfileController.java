package com.is1.proyecto.controllers;

import com.is1.proyecto.services.ProfileService;
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