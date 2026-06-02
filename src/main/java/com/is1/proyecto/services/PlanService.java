package com.is1.proyecto.services;

import com.is1.proyecto.models.Plan;

public class PlanService {

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
}