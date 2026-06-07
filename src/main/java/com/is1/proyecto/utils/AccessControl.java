package com.is1.proyecto.utils;

import static spark.Spark.halt;

import spark.Request;
import spark.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class AccessControl {
    
    public static void checkSession(Request req, Response res){
        Boolean loggedIn = (Boolean) req.session().attribute("loggedIn");
        String currentUsername = req.session().attribute("currentUserUsername");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=" + URLEncoder.encode("Acceso no autorizado.", StandardCharsets.UTF_8));
            halt(); 
            return;
        }
    }

    private static void checkRoleAccess(Request req, Response res, String role, String errorMsg){
        checkSession(req, res);

        Boolean hasRole = (Boolean) req.session().attribute(role);
        if (hasRole == null || !hasRole) {
            System.out.println("[DEBUG]: " + errorMsg);
            res.redirect("/dashboard?error=" + URLEncoder.encode("Acceso denegado. " + errorMsg, StandardCharsets.UTF_8));
            halt(); 
        }
    }

    public static void checkAdminAccess(Request req, Response res) {
        checkRoleAccess(req, res, "isAdmin", "Solo el administrador puede acceder.");
    }

    public static void checkStudentAccess(Request req, Response res) {
        checkRoleAccess(req, res, "isStudent", "Solo los estudiantes pueden acceder.");
    }

    public static void checkTeacherAccess(Request req, Response res) {
        checkRoleAccess(req, res, "isTeacher", "Solo los profesores pueden acceder.");
    }

}
