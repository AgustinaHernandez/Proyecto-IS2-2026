package com.is1.proyecto.services;
import com.is1.proyecto.models.Career;

import org.javalite.activejdbc.Base;

public class CareerService {

    public static String createCareer(String name){
        if (name == null || name.isEmpty()){
            return "Todos los campos son requeridos.";
        }

        String result = name.replaceAll("[^\\p{L}\\p{Nd}\\s]", "");
        if(result.length() != name.length())
            return "El nombre no puede contener caracteres especiales.";
        
        try {
            Base.openTransaction();
            Career nc = new Career();
            nc.set("name", name);
            nc.saveIt();
            Base.commitTransaction();               
            return "Carrera "+name+" registrada correctamente.";
        } catch (Exception e) {
            Base.rollbackTransaction(); // Si falla algo deshace
            e.printStackTrace(); // Imprime el stack trace para depuración.   
            return "ERROR: id de carrera ya existente o error interno.";
        }
    }

}