package com.is1.proyecto.controllers;

import com.is1.proyecto.services.CareerService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CareerController {

    // GET
    public static ModelAndView renderCreateForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        model.put("tituloPagina", "Alta de carrera");
        return new ModelAndView(model, "career_form.mustache");
    }

    // POST
    public static Object handleCreateCareer(Request req, Response res) {
        String name = req.queryParams("name").trim();
        
        // Llamamos al SERVICIO
        String errorMsg = CareerService.createCareer(name);

        // Evalua qué nos devolvió el servicio
        if (errorMsg != null) {
            String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            res.redirect("/career/create?error=" + encodedError);
        } else {
            res.status(201);
            String successMsg = URLEncoder.encode("Carrera " + name + " registrada correctamente.", StandardCharsets.UTF_8);
            res.redirect("/career/create?message=" + successMsg);
        }
        return "";
    }
}
