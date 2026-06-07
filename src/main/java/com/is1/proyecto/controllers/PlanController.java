package com.is1.proyecto.controllers;

import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.services.PlanService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
     */

    // GET
    public static ModelAndView renderQueryForm(Request req, Response res) {
        // Obtenemos los planes con sus carreras para la vista
        List<Plan> plans = Plan.findAll().include(Career.class);
        Map<String, Object> model = new HashMap<>();
        model.put("plans", plans);
        
        return new ModelAndView(model, "plans.mustache");
    }

    // POST
    public static ModelAndView handleQueryPlan(Request req, Response res) {
        String planId = req.queryParams("plan_id");
        Map<String, Object> model = new HashMap<>();
        Plan plan = Plan.findById(planId);

        model.put("plan", plan);
        //Materias pertenecientes al plan, sus correlativas y condiciones para cursar y rendir
        String subjectsQuery = 
            "SELECT s.id AS subj_id, s.code, s.name, corr.code AS correlative_code, " +
                "c.course_condition, c.exam_condition " +
            "FROM subject_belongs_plan sb " +
            "INNER JOIN subjects s ON s.id = sb.subject_id " +
            "LEFT JOIN conditions c ON c.subject_id = sb.subject_id " +
            "LEFT JOIN subjects corr ON c.correlative_id = corr.id " +
            "WHERE sb.plan_id = ? " +
            "ORDER BY s.code, corr.code"; 

        List<Map<String, Object>> processedSubjects = PlanService.listPlanSubjects(planId, subjectsQuery);
        model.put("subjects", processedSubjects);

        return new ModelAndView(model, "plan_details.mustache");
    }

}