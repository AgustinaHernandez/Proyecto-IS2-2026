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

/** Rutas --------------------------
 * /plan/new (GET/POST), 
 * /plans (GET/POST), 
 * /plan/update (GET/POST).
 */

public class PlanController {

    
    /**
     *  ---------------- PLAN CREATE ------------------------------------------
     */

    // GET
    public static ModelAndView renderCreateForm(Request req, Response res){
        List<Plan> plans = Plan.findAll().include(Career.class);

        List<Career> careers = Career.findAll();

        Map<String, Object> model = Map.of(
            "plans", plans,
            "careers",careers,
            "tituloPagina", "Nuevo plan",
            "errorMessage", req.queryParamOrDefault("errorMessage", ""),
            "successMessage", req.queryParamOrDefault("successMessage", "")
        );
       
        return new ModelAndView(model, "plan_new.mustache");
    }

    // POST
    public static Object handleCreatePlan(Request req, Response res){
        String careerId = req.queryParams("career_id"); // id de la carrera seleccionada
        String statePlan = req.queryParams("state");   //estado del plan
        String versionPlan = req.queryParams("version"); // version del plan
        
        String errorMsg = PlanService.createNewPlan(careerId, statePlan, versionPlan);
       
        if (errorMsg != null) {
            String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            res.redirect("/plan/new?errorMessage=" + encodedError);
        } else {
            String successMsg = URLEncoder.encode("Plan "+versionPlan+" registrado correctamente.",StandardCharsets.UTF_8);
            res.redirect("/plan/new?successMessage=" + successMsg);
        }

        return errorMsg;
    }




    /**
     *  ---------------- PLAN UPDATE ------------------------------------------
     */

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


    /**
     *  ---------------- LIST PLAN ------------------------------------------
     *  (refactorizar)
     */


}