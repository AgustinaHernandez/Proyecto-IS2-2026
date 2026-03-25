package com.is1.proyecto.models;             // Laburando

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("plans")                            //Incompleto revisar career_id 
public class Plan extends Model {

    public Integer getID() {
        return getInteger("id");
    }

    public String getVersion() {
        return getString("version");
    }

    public void setVersion(String version) {
        set("version", version);
    }
}