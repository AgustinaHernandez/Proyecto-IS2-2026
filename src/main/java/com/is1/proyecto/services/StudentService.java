package com.is1.proyecto.services;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Student;

/** Rutas --------------------------
 * /student/create (GET/POST), 
 * /student/new (GET/POST), 
 * /student/delete (GET/POST)
 */


public class StudentService {
    /**
     *  ---------------- STUDENT DELETE ------------------------------------------
     */
    public static String deleteStudent(String studentId) {

        if(studentId == null || studentId.isEmpty()){
            return "Error: Faltan datos obligatorios";
        }

        try {
            Base.openTransaction();
            Student st = Student.findById(Integer.parseInt(studentId));
            if(st != null && st.delete()){
                Base.commitTransaction();
                return null; //Creación correcta

            } else {
                Base.rollbackTransaction();
                return "Error: Estudiante con código [" + studentId + "] no encontrado";
            }

        } catch(Exception e){
            Base.rollbackTransaction();
            e.printStackTrace();
            return "Error al eliminar el estudiante [" + studentId + "]";
        }
    }
}
