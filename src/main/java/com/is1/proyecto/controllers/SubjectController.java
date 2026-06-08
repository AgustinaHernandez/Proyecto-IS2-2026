package com.is1.proyecto.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Plan;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.services.SubjectService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/** Rutas --------------------------
 * /subject/create (GET/POST), 
 */
public class SubjectController {
    public static ModelAndView renderSubjectCreation(Request req, Response res){
        // select de todos los profesores con sus datos de la tabla persona
        List<Teacher> teachers = Teacher.findAll().include(Person.class);
        // buscamos los planes
        List<Plan> plans = Plan.findAll().include(Career.class); 
        // mapeo para pasarle al mustache luego
        Map<String, Object> model = Map.of(
            "tituloPagina", "Alta de materia",
            "teachers", teachers,
            "plans", plans, // agregar planes al modelo
            "errorMessage", req.queryParamOrDefault("errorMessage", ""),
            "successMessage", req.queryParamOrDefault("successMessage", "")
        );
        return new ModelAndView(model, "subject_form.mustache");
    }

    public static Object handleSubjectCreation(Request req, Response res){
        String id = req.queryParams("code"); 
        String name = req.queryParams("name");
        String respId = req.queryParams("responsible_id");
        String planId = req.queryParams("plan_id");

        if (id == null || name == null || respId == null || id.isEmpty() || name.isEmpty() || planId == null || planId.isEmpty()) {
            res.redirect("/subject/create?error=" + URLEncoder.encode("Faltan datos obligatorios", StandardCharsets.UTF_8));
            return "";
        }

        String errorMsg = SubjectService.createSubject(id, name, respId, planId);

        if(errorMsg != null){
            res.redirect("/subject/create?errorMessage=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
        }else{
            res.redirect("/subject/create?successMessage=" + URLEncoder.encode("Se agregó la materia existosamente.", StandardCharsets.UTF_8));
        }

        return "";
    }
}
