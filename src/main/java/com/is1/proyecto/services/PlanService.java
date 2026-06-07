package com.is1.proyecto.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Plan;

/** Rutas --------------------------
 * /plan/new (GET/POST), 
 * /plans (GET/POST), 
 * /plan/update (GET/POST). ---
 */

public class PlanService {


    /**
     *  ---------------- PLAN CREATE ------------------------------------------
     */

    public static String createNewPlan(String careerId, String statePlan, String versionPlan){

        // Validaciones básicas: campos no pueden ser nulos o vacíos.
        if (careerId == null || careerId.isEmpty() || 
            statePlan == null || statePlan.isEmpty() || 
            versionPlan == null || versionPlan.isEmpty())
        {
           return "Todos los campos son requeridos.";
        }

        try {
             // Intenta crear y guardar el nuevo plan de estudios  en la base de datos.
             Base.openTransaction();  // Iniciamos la transaccio    
             Plan np = new Plan(); // Crea una nueva instancia del modelo PLan.

             np.set("career_id",careerId);
             np.set("state",statePlan);
             np.set("version",versionPlan);
             np.saveIt(); 
             Base.commitTransaction();                  
                          
            return null; // se creó correctamente
        } catch (Exception e) {
            Base.rollbackTransaction(); // Si falla algo deshace
            String errorMsg = URLEncoder.encode("ERROR: id de plan de carrera ya existente o error interno.", StandardCharsets.UTF_8);
            return errorMsg;
        }

    }


    /**
     *  ---------------- PLAN UPDATE ------------------------------------------
     */

    public static String updatePlanState(String planIdStr, String newState) {
        if (planIdStr == null || planIdStr.isEmpty() || newState == null || newState.isEmpty()) {
            return "Debes seleccionar un plan y un estado.";
        }

        try {
            int planId = Integer.parseInt(planIdStr);
            
            Plan p = Plan.findById(planId);
            if (p != null) {
                p.set("state", newState);
                p.saveIt();
                return null;
            } else {
                return "El plan no existe en la base de datos.";
            }

        } catch (NumberFormatException e) {
            return "El ID del plan es inválido.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error interno al actualizar el plan.";
        }
    }


    /**
     *  ---------------- LIST PLAN ------------------------------------------
     */

    public static List<Map<String, Object>> listPlanSubjects(String planIdStr, String subjectsQuery){

        List<Map> rawSubjects = Base.findAll(subjectsQuery, planIdStr);

        List<Map<String, Object>> processedSubjects = new ArrayList<>();
        Object lastSubjectId = null;
        Map<String, Object> previousRow = null; // Nueva variable para rastrear la fila de arriba

        for (Map row : rawSubjects) {
            Map<String, Object> newRow = new HashMap<>(row);
            Object currentId = row.get("subj_id");
            // Por defecto, asumo que esta fila cierra el grupo y lleva borde
            newRow.put("has_border", true);
            if (currentId != null && currentId.equals(lastSubjectId)) {
                newRow.put("code", "");
                newRow.put("name", "");
                // la fila de arriba no va a dibujar la línea inferior
                if (previousRow != null) {
                    previousRow.put("has_border", false);
                }
            } else {
                lastSubjectId = currentId;
            }
            processedSubjects.add(newRow);
            previousRow = newRow; // Guarda la fila actual para la próxima vuelta
        }
        return processedSubjects;
    }

}