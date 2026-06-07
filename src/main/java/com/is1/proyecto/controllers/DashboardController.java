package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/** Rutas --------------------------
 * /dashboard (GET).
 */

public class DashboardController {
    public static ModelAndView renderDashboard(Request req, Response res){
        Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        
        // Verificar si el usuario ya inició sesión.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            res.redirect("/?error=Acceso no autorizado.");
            return null;
        }
        
        //Ver qué roles tiene y cuántos son
        boolean isStudent = (boolean) req.session().attribute("isStudent");
        boolean isRegularStudent = (boolean) req.session().attribute("isRegularStudent");
        boolean isTeacher = (boolean) req.session().attribute("isTeacher");
        boolean isAdmin = (boolean) req.session().attribute("isAdmin");
        int roleCount = (isAdmin ? 1 : 0) + (isStudent ? 1 : 0) + (isTeacher ? 1:0);

        //Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
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

        return new ModelAndView(model, "dashboard.mustache");
    }
}