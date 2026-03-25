package com.is1.proyecto.models;            // Laburando

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("subject_belongs_plan")                            //Incompleto revisar plans
public class Subject_Belongs_Plan extends Model {

    public Integer getID() {
        return getInteger("id");
    }


    
}