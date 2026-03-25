package com.is1.proyecto.models;        // Laburando

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("enrolled_plan")
public class Enrolled_Plan extends Model {

    public Integer getID() {
        return getInteger("id");
    }


    
}