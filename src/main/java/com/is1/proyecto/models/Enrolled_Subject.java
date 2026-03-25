package com.is1.proyecto.models;          // Laburando

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("enrolled_subject")
public class Enrolled_Subject extends Model {

    public Integer getID() {
        return getInteger("id");
    }


    
}