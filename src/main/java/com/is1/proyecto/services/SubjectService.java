package com.is1.proyecto.services;

import com.is1.proyecto.models.Plan;
import com.is1.proyecto.models.Subject;

import org.javalite.activejdbc.Base;

/** Rutas --------------------------
 * /subject/create (GET/POST), 
 */
public class SubjectService {
    public static String createSubject(String id, String name, String respId, String planId){
        try {
            Base.openTransaction();
            Subject s = new Subject();
            s.set("code", Integer.parseInt(id));
            s.set("name", name);
            s.set("responsible_id", Integer.parseInt(respId));
            if (s.saveIt()) {
                // si se guardó la materia, la asocio al plan
                Plan p = Plan.findById(Integer.parseInt(planId));
                if (p != null) {
                    s.add(p); // acá ActiveJDBC hace el insert a subject_belongs_plan, por el @Many2Many
                }
                Base.commitTransaction();
                return null;
            } else {
                Base.rollbackTransaction();
                
                return "Error de validación: " + s.errors();
            }
        } catch (Exception e) {
            Base.rollbackTransaction();
            e.printStackTrace();
            
            return "El código ya existe o es inválido";
        }
    }
}