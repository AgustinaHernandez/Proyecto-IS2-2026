package com.is1.proyecto.controllers;

import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.services.PlanService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PlanController {

    // GET
    public static ModelAndView renderUpdateForm(Request req, Response res) {
        // Obtenemos los planes con sus carreras para la vista
        List<Plan> plans = Plan.findAll().include(Career.class);

        Map<String, Object> model = Map.of(
            "plans", plans,
            "tituloPagina", "Modificación de plan",
            "errorMessage", req.queryParamOrDefault("errorMessage", ""),
            "successMessage", req.queryParamOrDefault("successMessage", "")
        );
        return new ModelAndView(model, "plan_update.mustache");
    }

    // POST
    public static Object handleUpdatePlan(Request req, Response res) {
        String planId = req.queryParams("plan_id");
        String nuevoEstado = req.queryParams("state");

        System.out.println("DEBUG POST PLAN: planId=[" + planId + "] | estado=[" + nuevoEstado + "]");

        // SERVICIO
        String errorMsg = PlanService.updatePlanState(planId, nuevoEstado);

        if (errorMsg != null) {
            String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            res.redirect("/plan/update?errorMessage=" + encodedError);
        } else {
            String successMsg = URLEncoder.encode("El plan fue actualizado con éxito :D", StandardCharsets.UTF_8);
            res.redirect("/plan/update?successMessage=" + successMsg);
        }
        return "";
    }
}