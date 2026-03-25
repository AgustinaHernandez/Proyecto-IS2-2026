package com.is1.proyecto.models;              // Laburando

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teaches")
public class Teach extends Model {

    public Integer getID() {
        return getInteger("id");
    }


    
}