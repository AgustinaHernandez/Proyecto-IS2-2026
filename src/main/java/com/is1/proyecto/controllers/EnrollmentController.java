package com.is1.proyecto.controllers;

import java.util.HashMap;
import java.util.Map;

import com.is1.proyecto.services.EnrollmentService;
import com.is1.proyecto.services.EnrollmentService.AvailableSubjectsResult;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/** Rutas --------------------------
 *  /student/subject-enroll (GET/POST).
 */
public class EnrollmentController {

    // GET
    public static ModelAndView renderEnrollmentForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        
        Object userIdAttr = req.session().attribute("userId");
        Long userId = Long.valueOf(userIdAttr.toString());
        String activeRole = req.session().attribute("activeRole");


        AvailableSubjectsResult result = EnrollmentService.getAvailableSubjects(userId);

        if (!result.success) {
            req.session().attribute("error", result.errorMessage);
            res.redirect("/dashboard");
            return null;
        }

        model.put("materias", result.subjects);
        model.put("hasMaterias", !result.subjects.isEmpty());
        model.put("username", req.session().attribute("currentUserUsername"));
        model.put("activeRole", activeRole);
        model.put("isActiveStudent", true);

        if (req.session().attribute("error") != null) {
            model.put("error", req.session().attribute("error"));
            req.session().removeAttribute("error");
        }
        if (req.session().attribute("success") != null) {
            model.put("success", req.session().attribute("success"));
            req.session().removeAttribute("success");
        }

        return new ModelAndView(model, "subject_enrollment.mustache");
    }

    // POST
    public static Object handleEnrollment(Request req, Response res) {
        Object userIdAttr = req.session().attribute("userId");
        Long userId = Long.valueOf(userIdAttr.toString());
        String subjectId = req.queryParams("subject_id");

        String errorMsg = EnrollmentService.enrollInSubject(userId, subjectId);

        if (errorMsg != null) {
            req.session().attribute("error", errorMsg);
        } else {
            req.session().attribute("success", "¡Te has inscripto con éxito a la materia!");
        }

        res.redirect("/student/subject-enroll");
        return null;
    }


}